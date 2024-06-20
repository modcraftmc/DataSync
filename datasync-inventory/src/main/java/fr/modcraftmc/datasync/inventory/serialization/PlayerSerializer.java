package fr.modcraftmc.datasync.inventory.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.modcraftmc.datasync.inventory.DatasyncInventory;
import fr.modcraftmc.datasync.inventory.References;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.codehaus.plexus.util.StringOutputStream;
import top.theillusivec4.curios.api.CuriosCapability;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PlayerSerializer {
    public static final String PLAYER_DATA_IDENTIFIER = "playerData";
    public static final String CURIOS_INVENTORY_IDENTIFIER = "curiosInventory";

    private static final Map<Capability, CapabilitySerializer> CAPABILITY_SERIALIZERS = new Hashtable<>();

    private static final List<Capability> CAPABILITIES_TO_SAVE_BY_DEFAULT = List.of(CapabilityBackpackWrapper.BACKPACK_WRAPPER_CAPABILITY);

    private static class CapabilitySerializer<T> {
        private final Capability<T> capability;
        private final Function<T, CompoundTag> serializer;
        private final BiConsumer<T, CompoundTag> deserializer;

        public CapabilitySerializer(Capability<T> capability, Function<T, CompoundTag> serializer, BiConsumer<T, CompoundTag> deserializer) {
            this.capability = capability;
            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        public Capability<T> getCapability() {
            return capability;
        }

        public Function<T, CompoundTag> getSerializer() {
            return serializer;
        }

        public BiConsumer<T, CompoundTag> getDeserializer() {
            return deserializer;
        }

        public CompoundTag serialize(T instance) {
            return serializer.apply(instance);
        }

        public void deserialize(T instance, CompoundTag tag) {
            deserializer.accept(instance, tag);
        }
    }

    public static <T> void registerSerializer(Capability<T> capability, Function<T, CompoundTag> serializer, BiConsumer<T, CompoundTag> deserializer) {
        CAPABILITY_SERIALIZERS.put(capability, new CapabilitySerializer(capability, serializer, deserializer));

    }

    static {
        if(ModList.get().isLoaded(References.SOPHISTICATED_BACKPACKS_MOD_ID)){
            DatasyncInventory.LOGGER.info("Sophisticated Backpacks mod is loaded, registering serializers for backpacks capabilities");
            registerSerializer(CapabilityBackpackWrapper.BACKPACK_WRAPPER_CAPABILITY, (backpackWrapper) -> {
                CompoundTag tag = new CompoundTag();

                UpgradeHandler upgradeHandler = backpackWrapper.getUpgradeHandler();
                ListTag upgrades = new ListTag();
                for (int i = 0; i < upgradeHandler.getSlots(); i++) {
                    ItemStack stack = upgradeHandler.getStackInSlot(i);
                    if(stack.isEmpty()) continue;
                    CompoundTag itemData = stack.save(new CompoundTag());
                    itemData.putInt("Slot", i);
                    upgrades.add(itemData);
                }
                tag.put("Upgrades", upgrades);

                InventoryHandler itemHandler = backpackWrapper.getInventoryHandler();
                ListTag inventory = new ListTag();
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack stack = itemHandler.getStackInSlot(i);
                    if(stack.isEmpty()) continue;
                    CompoundTag itemData = stack.save(new CompoundTag());
                    itemData.putInt("UncappedCount", stack.getCount());
                    itemData.putInt("Slot", i);
                    inventory.add(itemData);
                }
                tag.put("Items", inventory);

                Optional<IEnergyStorage> optionalEnergyHandler = backpackWrapper.getEnergyStorage();
                optionalEnergyHandler.ifPresent((energyHandler) -> {
                    tag.putInt("Energy", energyHandler.getEnergyStored());
                });

                Optional<IStorageFluidHandler> optionalFluidHandler = backpackWrapper.getFluidHandler();
                optionalFluidHandler.ifPresent((fluidHandler) ->{
                    ListTag fluids = new ListTag();
                    for (int i = 0; i < fluidHandler.getTanks(); i++) {
                        fluids.add(fluidHandler.getFluidInTank(i).writeToNBT(new CompoundTag()));
                    }
                });

                return tag;
            }, (backpackWrapper, tag) -> {
                UpgradeHandler upgradeHandler = backpackWrapper.getUpgradeHandler();
                for (int i = 0; i < upgradeHandler.getSlots(); i++) {
                    upgradeHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
                ListTag upgrades = tag.getList("Upgrades", 10);
                for (int i = 0; i < upgrades.size(); i++) {
                    CompoundTag itemData = upgrades.getCompound(i);
                    upgradeHandler.setStackInSlot(itemData.getInt("Slot"), ItemStack.of(itemData));
                }

                InventoryHandler itemHandler = backpackWrapper.getInventoryHandler();
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
                ListTag inventory = tag.getList("Items", 10);
                for (int i = 0; i < inventory.size(); i++) {
                    CompoundTag itemData = inventory.getCompound(i);
                    ItemStack stack = ItemStack.of(itemData);
                    stack.setCount(itemData.getInt("UncappedCount"));
                    itemHandler.setStackInSlot(itemData.getInt("Slot"), stack);
                }

                Optional<IEnergyStorage> optionalEnergyHandler = backpackWrapper.getEnergyStorage();
                optionalEnergyHandler.ifPresent((energyHandler) -> {
                    energyHandler.extractEnergy(energyHandler.getEnergyStored(), false);
                    energyHandler.receiveEnergy(tag.getInt("Energy"), false);
                });

                Optional<IStorageFluidHandler> optionalFluidHandler = backpackWrapper.getFluidHandler();
                optionalFluidHandler.ifPresent((fluidHandler) ->{
                    ListTag fluids = tag.getList("Fluids", 10);
                    for (int i = 0; i < fluids.size(); i++) {
                        fluidHandler.drain(fluidHandler.getFluidInTank(i), IFluidHandler.FluidAction.EXECUTE, true);
                        fluidHandler.fill(FluidStack.loadFluidStackFromNBT(fluids.getCompound(i)), IFluidHandler.FluidAction.EXECUTE, true);
                    }
                });
            });
        }
    }

    public static String serializePlayer(ServerPlayer player){
        CompoundTag result = new CompoundTag();

        savePlayer(player, result);

        String nbt = SerializationUtil.ToString(result);
        DatasyncInventory.LOGGER.info("Player data : " + result.getAsString());
        DatasyncInventory.LOGGER.info("Serialized player data: " + nbt);
        return nbt;
    }

    public static void savePlayer(ServerPlayer player, CompoundTag nbt){
        CompoundTag playerTag = new CompoundTag();
        player.getFoodData().addAdditionalSaveData(playerTag);
        playerTag.putFloat("Health", player.getHealth());
        playerTag.putFloat("AbsorptionAmount", player.getAbsorptionAmount());
        playerTag.put("Attributes", player.getAttributes().save());
        playerTag.put("Inventory", savePlayerInventory(player.getInventory()));
        playerTag.putInt("SelectedItemSlot", player.getInventory().selected);
        playerTag.putFloat("XpP", player.experienceProgress);
        playerTag.putInt("XpLevel", player.experienceLevel);
        playerTag.putInt("XpTotal", player.totalExperience);
        playerTag.putInt("Score", player.getScore());
        player.getAbilities().addSaveData(playerTag);
        playerTag.put("EnderItems", player.getEnderChestInventory().createTag());
        nbt.put(PLAYER_DATA_IDENTIFIER, playerTag);
        savePlayerCurios(player, nbt);
        savePlayerAdvancements(player, nbt);
    }

    public static ListTag savePlayerInventory(Inventory inventory){
        ListTag inventoryTag = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if(inventory.getItem(i).isEmpty()) continue;
            ItemStack stack = inventory.getItem(i);
            CompoundTag itemTag = getItemTagWithCapabilities(stack, CAPABILITIES_TO_SAVE_BY_DEFAULT);
            itemTag.putInt("Slot", i);
            inventoryTag.add(itemTag);
        }
        return inventoryTag;
    }

    public static CompoundTag getItemTagWithCapabilities(ItemStack stack, List<Capability> capabilities){
        CompoundTag itemTag = stack.save(new CompoundTag());
        CompoundTag capabilitiesTag = new CompoundTag();
        capabilities.forEach(capability -> {
            stack.getCapability(capability).ifPresent(cap -> {
                CapabilitySerializer serializer = CAPABILITY_SERIALIZERS.get(capability);
                if(serializer != null){
                    capabilitiesTag.put(capability.getName(), serializer.serialize(cap));
                }
            });
        });
        if(!capabilitiesTag.isEmpty())
            itemTag.put("CapabilitiesData", capabilitiesTag);
        return itemTag;
    }

    public static void savePlayerCurios(Player player, CompoundTag nbt){
        if(ModList.get().isLoaded(References.CURIOS_MOD_ID)) {
            player.getCapability(CuriosCapability.INVENTORY).ifPresent((itemHandler) -> {
                ListTag curiosInventory = itemHandler.saveInventory(false);
                nbt.put(CURIOS_INVENTORY_IDENTIFIER, curiosInventory);
            });
        }
    }

    public static void savePlayerAdvancements(ServerPlayer player, CompoundTag nbt){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        ListTag advancementsArray = new ListTag();
        server.getAdvancements().getAllAdvancements().forEach(advancement -> {
            ListTag criteriaArray = new ListTag();
            server.getPlayerList().getPlayerAdvancements(player).getOrStartProgress(advancement).getCompletedCriteria().forEach(criterion -> {
                criteriaArray.add(StringTag.valueOf(criterion));
            });
            if(criteriaArray.size() == 0) return;

            CompoundTag advancementNBT = new CompoundTag();
            advancementNBT.putString("advancement", advancement.getId().toString());
            advancementNBT.put("criteria", criteriaArray);

            advancementsArray.add(advancementNBT);
        });

        nbt.put("advancements", advancementsArray);
    }

    public static void deserializePlayer(String nbt, ServerPlayer player){
        CompoundTag compoundTag = SerializationUtil.ToNbt(nbt);
        loadPlayer(compoundTag, player);
    }

    public static void loadPlayer(CompoundTag nbt, ServerPlayer player) {
        CompoundTag playerTag = nbt.getCompound(PLAYER_DATA_IDENTIFIER);

        player.getFoodData().readAdditionalSaveData(playerTag);
        player.setAbsorptionAmount(playerTag.getFloat("AbsorptionAmount"));
        if (playerTag.contains("Attributes", Tag.TAG_LIST) && player.level != null && !player.level.isClientSide) {
            player.getAttributes().load(playerTag.getList("Attributes", Tag.TAG_COMPOUND));
        }
        if (playerTag.contains("Health", Tag.TAG_ANY_NUMERIC)) {
            player.setHealth(playerTag.getFloat("Health"));
        }
        ListTag listtag = playerTag.getList("Inventory", Tag.TAG_COMPOUND);
        loadPlayerInventory(listtag, player.getInventory());
        player.getInventory().selected = playerTag.getInt("SelectedItemSlot");
        player.experienceProgress = playerTag.getFloat("XpP");
        player.experienceLevel = playerTag.getInt("XpLevel");
        player.totalExperience = playerTag.getInt("XpTotal");
        player.setScore(playerTag.getInt("Score"));
        player.getAbilities().loadSaveData(playerTag);
        if (playerTag.contains("EnderItems", Tag.TAG_LIST)) {
            player.getEnderChestInventory().fromTag(playerTag.getList("EnderItems", Tag.TAG_COMPOUND));
        }

        loadPlayerAdvancements(nbt, player);
        loadPlayerCurios(nbt, player);

        player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected)); // Update held item

        // also update data for the client
        player.gameMode.getGameModeForPlayer().updatePlayerAbilities(player.getAbilities());
        player.onUpdateAbilities();
    }

    public static void loadPlayerInventory(ListTag inventoryTag, Inventory inventory){
        inventory.clearContent();
        for (int i = 0; i < inventoryTag.size(); i++) {
            CompoundTag itemTag = inventoryTag.getCompound(i);
            inventory.setItem(itemTag.getInt("Slot"), loadItemStackWithCapabilities(itemTag, CAPABILITIES_TO_SAVE_BY_DEFAULT));
        }
    }

    public static ItemStack loadItemStackWithCapabilities(CompoundTag itemTag, List<Capability> capabilities){
        ItemStack stack = ItemStack.of(itemTag);
        CompoundTag capabilitiesTag = itemTag.getCompound("CapabilitiesData");
        capabilities.forEach(capability -> {
            stack.getCapability(capability).ifPresent(cap -> {
                CapabilitySerializer serializer = CAPABILITY_SERIALIZERS.get(capability);
                if(serializer != null){
                    serializer.deserialize(cap, capabilitiesTag.getCompound(capability.getName()));
                }
            });
        });
        return stack;
    }

    public static void loadPlayerCurios(CompoundTag nbt, Player player){
        if(ModList.get().isLoaded(References.CURIOS_MOD_ID)){
            ListTag curiosInventory = nbt.getList(CURIOS_INVENTORY_IDENTIFIER, Tag.TAG_COMPOUND);
            player.getCapability(CuriosCapability.INVENTORY).ifPresent((itemHandler) -> {
                itemHandler.saveInventory(true);
                itemHandler.loadInventory(curiosInventory);
            });
        }
    }

    public static void loadPlayerAdvancements(CompoundTag nbt, ServerPlayer player){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ListTag advancementsArray = nbt.getList("advancements", 10);
        Map<ResourceLocation, List<String>> completedAdvancements = new HashMap<>();
        if(advancementsArray == null) return;
        for (int i = 0; i < advancementsArray.size(); i++) {
            CompoundTag advancementElement = advancementsArray.getCompound(i);
            List<String> completedCriteria = new ArrayList<>();

            ListTag criteriaArray = advancementElement.getList("criteria", 8);
            for (int j = 0; j < criteriaArray.size(); j++) {
                completedCriteria.add(criteriaArray.getString(j));
            }

            completedAdvancements.put(new ResourceLocation(((StringTag) advancementElement.get("advancement")).getAsString()), completedCriteria);
        }

        PlayerAdvancements playerAdvancements = server.getPlayerList().getPlayerAdvancements(player);
        server.getAdvancements().getAllAdvancements().forEach(advancement -> {
            playerAdvancements.getOrStartProgress(advancement).getCompletedCriteria().forEach(criterion -> {
                if(!completedAdvancements.containsKey(advancement.getId()) || !completedAdvancements.get(advancement.getId()).contains(criterion)) {
                    playerAdvancements.getOrStartProgress(advancement).getCriterion(criterion).revoke();
                }
            });
            if(completedAdvancements.containsKey(advancement.getId())){
                playerAdvancements.getOrStartProgress(advancement).getRemainingCriteria().forEach(criterion -> {
                    if(completedAdvancements.get(advancement.getId()).contains(criterion)) {
                        playerAdvancements.getOrStartProgress(advancement).getCriterion(criterion).grant();
                    }
                });
            }
        });

        playerAdvancements.ensureAllVisible();
    }
}
