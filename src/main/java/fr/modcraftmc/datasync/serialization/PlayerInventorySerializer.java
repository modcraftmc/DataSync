package fr.modcraftmc.datasync.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Inventory;
public class PlayerInventorySerializer {
    public static JsonObject serializeInventory(Inventory inventory){
        JsonObject jsonObject = new JsonObject();

        JsonArray items = new JsonArray();
        inventory.items.forEach((itemStack) -> {
            items.add(SerializationUtil.ToJsonElement(itemStack));
        });

        JsonArray armor = new JsonArray();
        inventory.armor.forEach((itemStack) -> {
            armor.add(SerializationUtil.ToJsonElement(itemStack));
        });

        JsonElement offHand = SerializationUtil.ToJsonElement(inventory.offhand.get(0));

        jsonObject.add("items", items);
        jsonObject.add("armor", armor);
        jsonObject.add("offHand", offHand);

        return jsonObject;
    }

    public static void deserializeInventory(JsonObject jsonObject, Inventory inventory){
        JsonArray items = jsonObject.getAsJsonArray("items");
        for (int i = 0; i < items.size(); i++) {
            inventory.items.set(i, SerializationUtil.GetItemStack(items.get(i)));
        }

        JsonArray armor = jsonObject.getAsJsonArray("armor");
        for (int i = 0; i < armor.size(); i++) {
            inventory.armor.set(i, SerializationUtil.GetItemStack(armor.get(i)));
        }

        inventory.offhand.set(0, SerializationUtil.GetItemStack(jsonObject.get("offHand")));
    }


}
