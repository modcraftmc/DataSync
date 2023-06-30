package fr.modcraftmc.datasync.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.References;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.ServerLifecycleHooks;
import top.theillusivec4.curios.api.CuriosCapability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerSerializer {
    public static final String PLAYER_DATA_IDENTIFIER = "playerData";
    public static final String CURIOS_INVENTORY_IDENTIFIER = "curiosInventory";

    public static JsonObject serializePlayer(ServerPlayer player){
        JsonObject jsonObject = new JsonObject();

        savePlayerInventory(player, jsonObject);
        savePlayerCurios(player, jsonObject);
        savePlayerAdvancements(player, jsonObject);

        return jsonObject;
    }

    public static void savePlayerInventory(Player player, JsonObject jsonObject){
        CompoundTag playerTag = new CompoundTag();
        player.getFoodData().addAdditionalSaveData(playerTag);
        playerTag.putFloat("Health", player.getHealth());
        playerTag.putFloat("AbsorptionAmount", player.getAbsorptionAmount());
        playerTag.put("Attributes", player.getAttributes().save());
        playerTag.put("Inventory", player.getInventory().save(new ListTag()));
        playerTag.putInt("SelectedItemSlot", player.getInventory().selected);
        playerTag.putFloat("XpP", player.experienceProgress);
        playerTag.putInt("XpLevel", player.experienceLevel);
        playerTag.putInt("XpTotal", player.totalExperience);
        playerTag.putInt("Score", player.getScore());
        player.getAbilities().addSaveData(playerTag);
        playerTag.put("EnderItems", player.getEnderChestInventory().createTag());
        jsonObject.add(PLAYER_DATA_IDENTIFIER, SerializationUtil.ToJsonElement(playerTag));
    }

    public static void savePlayerCurios(Player player, JsonObject jsonObject){
        if(ModList.get().isLoaded(References.CURIOS_MOD_ID)) {
            player.getCapability(CuriosCapability.INVENTORY).ifPresent((itemHandler) -> {
                JsonArray curiosArray = new JsonArray();

                ListTag listTag = itemHandler.saveInventory(false);
                for (int i = 0; i < listTag.size(); i++) {
                    curiosArray.add(SerializationUtil.ToJsonElement(listTag.getCompound(i)));
                }
                jsonObject.add(CURIOS_INVENTORY_IDENTIFIER, curiosArray);
            });
        }
    }

    public static void savePlayerAdvancements(ServerPlayer player, JsonObject jsonObject){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        JsonArray advancementsArray = new JsonArray();
        server.getAdvancements().getAllAdvancements().forEach(advancement -> {
            JsonArray criteriaArray = new JsonArray();
            server.getPlayerList().getPlayerAdvancements(player).getOrStartProgress(advancement).getCompletedCriteria().forEach(criterion -> {
                criteriaArray.add(criterion);
            });
            if(criteriaArray.size() == 0) return;

            JsonObject advancementObject = new JsonObject();
            advancementObject.addProperty("advancement", advancement.getId().toString());
            advancementObject.add("criteria", criteriaArray);

            advancementsArray.add(advancementObject);
        });

        jsonObject.add("advancements", advancementsArray);
    }

    public static void deserializePlayer(JsonObject jsonObject, ServerPlayer player){
        loadPlayerAdvancements(jsonObject, player);
        loadPlayerCurios(jsonObject, player);
        loadPlayerInventory(jsonObject, player);
    }

    public static void loadPlayerInventory(JsonObject jsonObject, ServerPlayer player) {
        CompoundTag playerTag = SerializationUtil.GetNbt(jsonObject.get(PLAYER_DATA_IDENTIFIER));
        player.getFoodData().readAdditionalSaveData(playerTag);
        player.setAbsorptionAmount(playerTag.getFloat("AbsorptionAmount"));
        if (playerTag.contains("Attributes", 9) && player.level != null && !player.level.isClientSide) {
            player.getAttributes().load(playerTag.getList("Attributes", 10));
        }
        if (playerTag.contains("Health", 99)) {
            player.setHealth(playerTag.getFloat("Health"));
        }
        ListTag listtag = playerTag.getList("Inventory", 10);
        player.getInventory().load(listtag);
        player.getInventory().selected = playerTag.getInt("SelectedItemSlot");
        player.experienceProgress = playerTag.getFloat("XpP");
        player.experienceLevel = playerTag.getInt("XpLevel");
        player.totalExperience = playerTag.getInt("XpTotal");
        player.setScore(playerTag.getInt("Score"));
        player.getAbilities().loadSaveData(playerTag);
        if (playerTag.contains("EnderItems", 9)) {
            player.getEnderChestInventory().fromTag(playerTag.getList("EnderItems", 10));
        }
        player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected)); // Update held item
    }

    public static void loadPlayerCurios(JsonObject jsonObject, Player player){
        if(ModList.get().isLoaded(References.CURIOS_MOD_ID)){
            JsonArray curiosArray = jsonObject.getAsJsonArray(CURIOS_INVENTORY_IDENTIFIER);
            if(curiosArray != null){

                ListTag listTag = new ListTag();
                for (int i = 0; i < curiosArray.size(); i++) {
                    listTag.add(SerializationUtil.GetNbt(curiosArray.get(i)));
                }

                player.getCapability(CuriosCapability.INVENTORY).ifPresent((itemHandler) -> {
                    itemHandler.saveInventory(true);
                    itemHandler.loadInventory(listTag);
                });
            }
        }
    }

    public static void loadPlayerAdvancements(JsonObject jsonObject, ServerPlayer player){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        JsonArray advancementsArray = jsonObject.getAsJsonArray("advancements");
        Map<ResourceLocation, List<String>> completedAdvancements = new HashMap<>();
        if(advancementsArray == null) return;
        advancementsArray.forEach(advancementElement -> {
            JsonObject advancementJson = advancementElement.getAsJsonObject();
            List<String> completedCriteria = new ArrayList<>();

            JsonArray criteriaArray = advancementJson.getAsJsonArray("criteria");
            criteriaArray.forEach(criterionElement -> {
                completedCriteria.add(criterionElement.getAsString());
            });

            completedAdvancements.put(new ResourceLocation(advancementJson.get("advancement").getAsString()), completedCriteria);
        });

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
