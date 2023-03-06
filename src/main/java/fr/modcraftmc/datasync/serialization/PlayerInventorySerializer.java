package fr.modcraftmc.datasync.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosCapability;

public class PlayerInventorySerializer {
    public static JsonObject serializeInventory(Inventory inventory){
        JsonObject jsonObject = new JsonObject();

        if(ModList.get().isLoaded("curios")){
            inventory.player.getCapability(CuriosCapability.INVENTORY).ifPresent((itemHandler) -> {
                JsonArray curiosArray = new JsonArray();

                ListTag listTag = itemHandler.saveInventory(false);
                for (int i = 0; i < listTag.size(); i++) {
                    curiosArray.add(SerializationUtil.ToJsonElement(listTag.getCompound(i)));
                }

                jsonObject.add("curios", curiosArray);
            });
        }

        JsonArray inventoryItems = new JsonArray();
        for (int i = 0; i < inventory.items.size(); i++) {
            if(inventory.items.get(i).isEmpty())
                continue;
            JsonObject inventoryItem = SerializationUtil.ToJsonElement(inventory.items.get(i)).getAsJsonObject();
            inventoryItem.addProperty("slot", i);
            inventoryItems.add(inventoryItem);
        }

        JsonArray inventoryArmor = new JsonArray();
        inventory.armor.forEach((itemStack) -> {
            inventoryArmor.add(SerializationUtil.ToJsonElement(itemStack));
        });

        JsonElement inventoryOffHand = SerializationUtil.ToJsonElement(inventory.offhand.get(0));

        jsonObject.add("items", inventoryItems);
        jsonObject.add("armor", inventoryArmor);
        jsonObject.add("offHand", inventoryOffHand);

        return jsonObject;
    }

    public static void deserializeInventory(JsonObject jsonObject, Inventory inventory){
        if(ModList.get().isLoaded("curios")){
            JsonArray curiosArray = jsonObject.getAsJsonArray("curios");
            if(curiosArray != null){

                ListTag listTag = new ListTag();
                for (int i = 0; i < curiosArray.size(); i++) {
                    listTag.add(SerializationUtil.GetNbt(curiosArray.get(i)));
                }

                inventory.player.getCapability(CuriosCapability.INVENTORY).ifPresent((itemHandler) -> {
                    itemHandler.loadInventory(listTag);
                });
            }
        }

        JsonArray items = jsonObject.getAsJsonArray("items");
        for (int i = 0; i < items.size(); i++) {
            inventory.items.set(items.get(i).getAsJsonObject().get("slot").getAsInt(), SerializationUtil.GetItemStack(items.get(i)));
        }

        JsonArray armor = jsonObject.getAsJsonArray("armor");
        for (int i = 0; i < armor.size(); i++) {
            inventory.armor.set(i, SerializationUtil.GetItemStack(armor.get(i)));
        }

        inventory.offhand.set(0, SerializationUtil.GetItemStack(jsonObject.get("offHand")));
    }


}
