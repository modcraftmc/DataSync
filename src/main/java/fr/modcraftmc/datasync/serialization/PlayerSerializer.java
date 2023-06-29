package fr.modcraftmc.datasync.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.References;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosCapability;

public class PlayerSerializer {
    public static final String PLAYER_DATA_IDENTIFIER = "playerData";
    public static final String CURIOS_INVENTORY_IDENTIFIER = "curiosInventory";

    public static JsonObject serializePlayer(Player player){
        JsonObject jsonObject = new JsonObject();

        savePlayerInventory(player, jsonObject);
        savePlayerCurios(player, jsonObject);

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

    public static void deserializePlayer(JsonObject jsonObject, ServerPlayer player){
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
}
