package fr.modcraftmc.datasync.inventory.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.modcraftmc.datasync.inventory.DatasyncInventory;
import fr.modcraftmc.datasync.inventory.References;
import fr.modcraftmc.datasync.inventory.mixin.AttachmentHolderMixin;
import fr.modcraftmc.datasync.inventory.mixin.EntityMixin;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.apache.logging.log4j.util.TriConsumer;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.*;
import java.util.function.BiFunction;

public class PlayerSerializer {
    public static final String PLAYER_DATA_IDENTIFIER = "playerData";
    public static final String CURIOS_INVENTORY_IDENTIFIER = "curiosInventory";

    private static final Map<ItemCapability, CapabilitySerializer> CAPABILITY_SERIALIZERS = new Hashtable<>();
    private static final List<ItemCustomSerializer> CUSTOM_SERIALIZERS = new ArrayList<>();

    private static final List<ItemCapability> CAPABILITIES_TO_SAVE_BY_DEFAULT = new ArrayList<>();
    private static final Codec<ItemStack> NO_COUNT_LIMIT_ITEM_STACK_CODEC = Codec.lazyInitialized(() -> { // C'est du scotch
        return RecordCodecBuilder.create((p_347288_) -> {
            return p_347288_.group(ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder), Codec.INT.fieldOf("count").forGetter(ItemStack::getCount), DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter((p_330103_) -> {
                return ((PatchedDataComponentMap) p_330103_.getComponents()).asPatch();
            })).apply(p_347288_, ItemStack::new);
        });
    });

    private static class CapabilitySerializer<T> {
        private final ItemCapability<T, Void> capability;
        private final BiFunction<HolderLookup.Provider, ItemStack, CompoundTag> serializer;
        private final TriConsumer<HolderLookup.Provider, ItemStack, CompoundTag> deserializer;

        public CapabilitySerializer(ItemCapability<T, Void> capability, BiFunction<HolderLookup.Provider, ItemStack, CompoundTag> serializer, TriConsumer<HolderLookup.Provider, ItemStack, CompoundTag> deserializer) {
            this.capability = capability;
            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        public ItemCapability<T, Void> getCapability() {
            return capability;
        }

        public BiFunction<HolderLookup.Provider, ItemStack, CompoundTag> getSerializer() {
            return serializer;
        }

        public TriConsumer<HolderLookup.Provider, ItemStack, CompoundTag> getDeserializer() {
            return deserializer;
        }

        public CompoundTag serialize(HolderLookup.Provider lookup, ItemStack itemStack) {
            return serializer.apply(lookup, itemStack);
        }

        public void deserialize(HolderLookup.Provider lookup, ItemStack itemStack, CompoundTag tag) {
            deserializer.accept(lookup, itemStack, tag);
        }
    }

    private static class ItemCustomSerializer<I extends Item>{
        private final Class<I> itemClass;
        private final BiFunction<HolderLookup.Provider, ItemStack, CompoundTag> serializer;
        private final TriConsumer<HolderLookup.Provider, ItemStack, CompoundTag> deserializer;

        public ItemCustomSerializer(Class<I> clazz, BiFunction<HolderLookup.Provider, ItemStack, CompoundTag> serializer, TriConsumer<HolderLookup.Provider, ItemStack, CompoundTag> deserializer) {
            itemClass = clazz;
            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        public CompoundTag serialize(HolderLookup.Provider lookup, ItemStack itemStack) {
            return serializer.apply(lookup, itemStack);
        }

        public void deserialize(HolderLookup.Provider lookup, ItemStack itemStack, CompoundTag tag) {
            deserializer.accept(lookup, itemStack, tag);
        }

        public boolean isItemSerializer(Item item) {
            return item.getClass() == itemClass;
        }
    }

    public static <T> void registerCapabilitySerializer(ItemCapability<T, Void> capability, BiFunction<HolderLookup.Provider, ItemStack, CompoundTag> serializer, TriConsumer<HolderLookup.Provider, ItemStack, CompoundTag> deserializer) {
        CAPABILITY_SERIALIZERS.put(capability, new CapabilitySerializer(capability, serializer, deserializer));
    }
    public static <I extends Item> void registerCustomSerializer(Class<I> clazz, BiFunction<HolderLookup.Provider, ItemStack, CompoundTag> serializer, TriConsumer<HolderLookup.Provider, ItemStack, CompoundTag> deserializer) {
        CUSTOM_SERIALIZERS.add(new ItemCustomSerializer(clazz, serializer, deserializer));
    }

    static {
        if(ModList.get().isLoaded(References.SOPHISTICATED_BACKPACKS_MOD_ID)){
            DatasyncInventory.LOGGER.info("Sophisticated Backpacks mod is loaded, registering serializers for backpacks capabilities");
            registerCustomSerializer(BackpackItem.class, (lookup, backpackItemStack) -> {
                CompoundTag tag = new CompoundTag();

                IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpackItemStack);
                UpgradeHandler upgradeHandler = backpackWrapper.getUpgradeHandler();
                ListTag upgrades = new ListTag();
                for (int i = 0; i < upgradeHandler.getSlots(); i++) {
                    ItemStack stack = upgradeHandler.getStackInSlot(i);
                    if(stack.isEmpty()) continue;
                    CompoundTag itemData = new CompoundTag();
                    itemData.put("Item", saveItemStack(lookup, stack, true, true));
                    itemData.putInt("Slot", i);
                    upgrades.add(itemData);
                }
                tag.put("Upgrades", upgrades);

                InventoryHandler itemHandler = backpackWrapper.getInventoryHandler();
                ListTag inventory = new ListTag();
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack stack = itemHandler.getStackInSlot(i);
                    if(stack.isEmpty()) continue;
                    CompoundTag itemData = new CompoundTag();
                    itemData.put("Item", saveItemStack(lookup, stack, true, true));
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
                        fluids.add(fluidHandler.getFluidInTank(i).save(lookup));
                    }
                });

                return tag;
            }, (lookup, backpackItemStack, tag) -> {
                IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpackItemStack);
                UpgradeHandler upgradeHandler = backpackWrapper.getUpgradeHandler();
                for (int i = 0; i < upgradeHandler.getSlots(); i++) {
                    upgradeHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
                ListTag upgrades = tag.getList("Upgrades", 10);
                for (int i = 0; i < upgrades.size(); i++) {
                    CompoundTag itemData = upgrades.getCompound(i);
                    upgradeHandler.setStackInSlot(itemData.getInt("Slot"), loadItemStack(lookup, itemData.getCompound("Item"), true, true));
                }

                InventoryHandler itemHandler = backpackWrapper.getInventoryHandler();
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
                ListTag inventory = tag.getList("Items", 10);
                for (int i = 0; i < inventory.size(); i++) {
                    CompoundTag itemData = inventory.getCompound(i);
                    ItemStack stack = loadItemStack(lookup, itemData.getCompound("Item"), true, true);
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
                        fluidHandler.fill(FluidStack.parse(lookup, fluids.getCompound(i)).get(), IFluidHandler.FluidAction.EXECUTE, true);
                    }
                });
            });
        }
    }

    public static String serializePlayer(ServerPlayer player){
        CompoundTag result = new CompoundTag();

        savePlayer(player, result);

        String nbt = SerializationUtil.ToString(result);
        return nbt;
    }

    public static void savePlayer(ServerPlayer player, CompoundTag nbt){
        HolderLookup.Provider lookup = player.registryAccess();

        CompoundTag playerTag = new CompoundTag();
        player.getFoodData().addAdditionalSaveData(playerTag);
        playerTag.putFloat("Health", player.getHealth());
        playerTag.putFloat("AbsorptionAmount", player.getAbsorptionAmount());
        playerTag.put("Attributes", player.getAttributes().save());
        playerTag.put("Inventory", savePlayerInventory(lookup, player.getInventory()));
        playerTag.putInt("SelectedItemSlot", player.getInventory().selected);
        playerTag.putFloat("XpP", player.experienceProgress);
        playerTag.putInt("XpLevel", player.experienceLevel);
        playerTag.putInt("XpTotal", player.totalExperience);
        playerTag.putInt("Score", player.getScore());
        playerTag.putInt("GameMode", player.gameMode.getGameModeForPlayer().getId());
        player.getAbilities().addSaveData(playerTag);
        playerTag.put("EnderItems", player.getEnderChestInventory().createTag(lookup));

        CompoundTag attachments = player.serializeAttachments(lookup);
        if (attachments != null) playerTag.put(ServerPlayer.ATTACHMENTS_NBT_KEY, attachments);
        if (((EntityMixin) player).datasync_getPersistentData() != null) playerTag.put("NeoForgeData", ((EntityMixin) player).datasync_getPersistentData().copy());

        nbt.put(PLAYER_DATA_IDENTIFIER, playerTag);
        savePlayerCurios(lookup, player, nbt);
        savePlayerAdvancements(player, nbt);
    }

    public static ListTag savePlayerInventory(HolderLookup.Provider lookup, Inventory inventory){
        ListTag inventoryTag = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if(inventory.getItem(i).isEmpty()) continue;
            ItemStack stack = inventory.getItem(i);
            CompoundTag itemTag = saveItemStack(lookup, stack, true, true);
            itemTag.putInt("Slot", i);
            inventoryTag.add(itemTag);
        }
        return inventoryTag;
    }

    public static CompoundTag saveItemStack(HolderLookup.Provider lookup, ItemStack stack, boolean withCustomSerializers, boolean withCapabilities) {
        CompoundTag itemData = new CompoundTag();

        DataResult<Tag> dataResult = NO_COUNT_LIMIT_ITEM_STACK_CODEC.encodeStart(lookup.createSerializationContext(NbtOps.INSTANCE), stack);

        if (dataResult.error().isPresent()) {
            DatasyncInventory.LOGGER.error("Failed to serialize item stack: " + dataResult.error().get().message());
            return itemData;
        }

        itemData.put("Item", dataResult.getOrThrow());

        if(withCustomSerializers){
            saveCustomSerializers(lookup, stack).ifPresent((customData) -> {
                itemData.put("CustomData", customData);
            });
        }

        if(withCapabilities){
            saveCapabilities(lookup, stack, CAPABILITIES_TO_SAVE_BY_DEFAULT).ifPresent((capabilitiesData) -> {
                itemData.put("CapabilitiesData", capabilitiesData);
            });
        }

        return itemData;
    }

    public static Optional<CompoundTag> saveCustomSerializers(HolderLookup.Provider lookup, ItemStack stack){
        for (ItemCustomSerializer customSerializer : CUSTOM_SERIALIZERS) {
            if(customSerializer.isItemSerializer(stack.getItem())){
                return Optional.of(customSerializer.serialize(lookup, stack));
            }
        }

        return Optional.empty();
    }

    public static Optional<CompoundTag> saveCapabilities(HolderLookup.Provider lookup, ItemStack stack, List<ItemCapability> capabilities){
        CompoundTag capabilitiesTag = new CompoundTag();
        capabilities.forEach(capability -> {
            if(stack.getCapability(capability) == null) return;

            CapabilitySerializer serializer = CAPABILITY_SERIALIZERS.get(capability);
            if(serializer != null){
                capabilitiesTag.put(capability.name().toString(), serializer.serialize(lookup, stack));
            }
        });
        if(!capabilitiesTag.isEmpty())
            return Optional.of(capabilitiesTag);
        return Optional.empty();
    }

    public static CompoundTag saveItemHandler(HolderLookup.Provider lookup, IItemHandler handler){
        CompoundTag handlerTag = new CompoundTag();
        ListTag itemList = new ListTag();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if(stack.isEmpty()) continue;
            CompoundTag itemTag = saveItemStack(lookup, stack, true, true);
            itemTag.putInt("Slot", i);
            itemList.add(itemTag);
        }

        handlerTag.put("Items", itemList);
        handlerTag.putInt("Size", handler.getSlots());
        return handlerTag;
    }

    public static ItemStack loadItemStack(HolderLookup.Provider lookup, CompoundTag tag, boolean withCustomSerializers, boolean withCapabilities) {
        DataResult<ItemStack> dataResult = NO_COUNT_LIMIT_ITEM_STACK_CODEC.parse(lookup.createSerializationContext(NbtOps.INSTANCE), tag.get("Item"));

        //TODO: better handle error?
        if (dataResult.error().isPresent()) {
            DatasyncInventory.LOGGER.error("Failed to deserialize item stack: " + dataResult.error().get().message());
            ItemStack stack = Items.BARRIER.getDefaultInstance();
            return stack;
        }

        ItemStack stack = dataResult.getOrThrow();

        if(withCustomSerializers){
            loadCustomSerializers(lookup, stack, tag.getCompound("CustomData"));
        }

        if(withCapabilities){
            loadCapabilities(lookup, stack, CAPABILITIES_TO_SAVE_BY_DEFAULT, tag.getCompound("CapabilitiesData"));
        }

        return stack;
    }

    public static void loadCustomSerializers(HolderLookup.Provider lookup, ItemStack stack, CompoundTag customData){
        for (ItemCustomSerializer customSerializer : CUSTOM_SERIALIZERS) {
            if(customSerializer.isItemSerializer(stack.getItem())){
                customSerializer.deserialize(lookup, stack, customData);
                return;
            }
        }
    }

    public static void loadCapabilities(HolderLookup.Provider lookup, ItemStack stack, List<ItemCapability> capabilities, CompoundTag capabilitiesData){
        capabilities.forEach(capability -> {
            if(stack.getCapability(capability) == null) return;

            CapabilitySerializer serializer = CAPABILITY_SERIALIZERS.get(capability);
            if(serializer != null){
                serializer.deserialize(lookup, stack, capabilitiesData.getCompound(capability.name().toString()));
            }
        });
    }

    public static ItemStackHandler loadItemHandler(HolderLookup.Provider lookup, CompoundTag handlerTag){
        ItemStackHandler handler = new ItemStackHandler(handlerTag.getInt("Size"));
        ListTag itemList = handlerTag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            CompoundTag itemTag = itemList.getCompound(i);
            handler.setStackInSlot(itemTag.getInt("Slot"), loadItemStack(lookup, itemTag, true, true));
        }

        return handler;
    }

    public static void savePlayerCurios(HolderLookup.Provider lookup, Player player, CompoundTag nbt){
        if(ModList.get().isLoaded(References.CURIOS_MOD_ID)) {
            ICuriosItemHandler curiosItemHandler = player.getCapability(CuriosCapability.INVENTORY);
            if (curiosItemHandler == null) return;

            // from curios - start
            ListTag curiosInventory = new ListTag();
            for (Map.Entry<String, ICurioStacksHandler> entry : curiosItemHandler.getCurios().entrySet()) {
                CompoundTag tag = new CompoundTag();
                ICurioStacksHandler stacksHandler = entry.getValue();
                IDynamicStackHandler stacks = stacksHandler.getStacks();
                IDynamicStackHandler cosmetics = stacksHandler.getCosmeticStacks();
                tag.put("Stacks", saveItemHandler(lookup, stacks));
                tag.put("Cosmetics", saveItemHandler(lookup, cosmetics));
                tag.putString("Identifier", entry.getKey());
                curiosInventory.add(tag);
            }
            // from curios - end

            nbt.put(CURIOS_INVENTORY_IDENTIFIER, curiosInventory);
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
            advancementNBT.putString("advancement", advancement.id().toString());
            advancementNBT.put("criteria", criteriaArray);

            advancementsArray.add(advancementNBT);
        });

        nbt.put("advancements", advancementsArray);
    }

    public static void deserializePlayer(String nbt, ServerPlayer player){
        CompoundTag compoundTag = SerializationUtil.ToNbt(nbt);
        HolderLookup.Provider lookup = ServerLifecycleHooks.getCurrentServer().registryAccess();

        if (compoundTag == null) {
            DatasyncInventory.LOGGER.error("Failed to deserialize player data, compound tag is null");
            player.connection.disconnect(Component.literal("Datasync error: Failed to deserialize player data"));
            return;
        }
        loadPlayer(lookup, compoundTag, player);
    }

    public static void loadPlayer(HolderLookup.Provider lookup, CompoundTag nbt, ServerPlayer player) {
        CompoundTag playerTag = nbt.getCompound(PLAYER_DATA_IDENTIFIER);

        player.getFoodData().readAdditionalSaveData(playerTag);
        player.setAbsorptionAmount(playerTag.getFloat("AbsorptionAmount"));
        if (playerTag.contains("Attributes", Tag.TAG_LIST) && player.level() != null && !player.level().isClientSide) {
            player.getAttributes().load(playerTag.getList("Attributes", Tag.TAG_COMPOUND));
        }
        if (playerTag.contains("Health", Tag.TAG_ANY_NUMERIC)) {
            player.setHealth(playerTag.getFloat("Health"));
        }
        ListTag listtag = playerTag.getList("Inventory", Tag.TAG_COMPOUND);
        loadPlayerInventory(lookup, listtag, player.getInventory());
        player.getInventory().selected = playerTag.getInt("SelectedItemSlot");
        player.experienceProgress = playerTag.getFloat("XpP");
        player.experienceLevel = playerTag.getInt("XpLevel");
        player.totalExperience = playerTag.getInt("XpTotal");
        player.setScore(playerTag.getInt("Score"));
        player.setGameMode(GameType.byId(playerTag.getInt("GameMode")));
        player.getAbilities().loadSaveData(playerTag);
        if (playerTag.contains("EnderItems", Tag.TAG_LIST)) {
            player.getEnderChestInventory().fromTag(playerTag.getList("EnderItems", Tag.TAG_COMPOUND), lookup);
        }

        if (playerTag.contains("NeoForgeData", 10)) ((EntityMixin) player).datasync_setPersistentData(playerTag.getCompound("NeoForgeData"));
        if (playerTag.contains(AttachmentHolder.ATTACHMENTS_NBT_KEY, Tag.TAG_COMPOUND)) ((AttachmentHolderMixin) player).datasync_deserializeAttachments(player.registryAccess(), playerTag.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY));

        loadPlayerAdvancements(nbt, player);
        loadPlayerCurios(lookup, nbt, player);

        player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected)); // Update held item
        player.onUpdateAbilities(); // also update data for the client
    }

    public static void loadPlayerInventory(HolderLookup.Provider lookup, ListTag inventoryTag, Inventory inventory){
        inventory.clearContent();
        for (int i = 0; i < inventoryTag.size(); i++) {
            CompoundTag itemTag = inventoryTag.getCompound(i);
            inventory.setItem(itemTag.getInt("Slot"), loadItemStack(lookup, itemTag, true, true));
        }
    }

    //from Curios
    private static void loadStacks(ICurioStacksHandler stacksHandler, ItemStackHandler loaded,
                            IDynamicStackHandler stacks) {
        for (int j = 0; j < stacksHandler.getSlots() && j < loaded.getSlots(); j++) {
            ItemStack loadedStack = loaded.getStackInSlot(j);

            stacks.setStackInSlot(j, loadedStack);
        }
    }

    public static void loadPlayerCurios(HolderLookup.Provider lookup, CompoundTag nbt, Player player){
        if(ModList.get().isLoaded(References.CURIOS_MOD_ID)){
            ListTag curiosInventory = nbt.getList(CURIOS_INVENTORY_IDENTIFIER, Tag.TAG_COMPOUND);
            ICuriosItemHandler curiosItemHandler = player.getCapability(CuriosCapability.INVENTORY);
            if (curiosItemHandler == null) return;

            curiosItemHandler.saveInventory(true);
            // from curios - start
            for (int i = 0; i < curiosInventory.size(); i++) {
                CompoundTag tag = curiosInventory.getCompound(i);
                String identifier = tag.getString("Identifier");
                ICurioStacksHandler stacksHandler = curiosItemHandler.getCurios().get(identifier);

                if (stacksHandler != null) {
                    CompoundTag stacksData = tag.getCompound("Stacks");
                    ItemStackHandler loaded = loadItemHandler(lookup, stacksData);
                    IDynamicStackHandler stacks = stacksHandler.getStacks();

                    if (!stacksData.isEmpty()) {
                        loadStacks(stacksHandler, loaded, stacks);
                    }

                    stacksData = tag.getCompound("Cosmetics");
                    loaded = loadItemHandler(lookup, stacksData);
                    stacks = stacksHandler.getCosmeticStacks();

                    if (!stacksData.isEmpty()) {
                        loadStacks(stacksHandler, loaded, stacks);
                    }
                }
            }
            // from curios - end
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
            completedAdvancements.put(ResourceLocation.parse(advancementElement.getString("advancement")), completedCriteria);
        }

        PlayerAdvancements playerAdvancements = server.getPlayerList().getPlayerAdvancements(player);
        server.getAdvancements().getAllAdvancements().forEach(advancement -> {
            playerAdvancements.getOrStartProgress(advancement).getCompletedCriteria().forEach(criterion -> {
                if(!completedAdvancements.containsKey(advancement.id()) || !completedAdvancements.get(advancement.id()).contains(criterion)) {
                    playerAdvancements.getOrStartProgress(advancement).getCriterion(criterion).revoke();
                }
            });
            if(completedAdvancements.containsKey(advancement.id())){
                playerAdvancements.getOrStartProgress(advancement).getRemainingCriteria().forEach(criterion -> {
                    if(completedAdvancements.get(advancement.id()).contains(criterion)) {
                        playerAdvancements.getOrStartProgress(advancement).getCriterion(criterion).grant();
                    }
                });
            }
        });
    }
}
