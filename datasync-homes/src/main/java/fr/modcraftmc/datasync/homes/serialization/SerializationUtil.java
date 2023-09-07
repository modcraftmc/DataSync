package fr.modcraftmc.datasync.homes.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;


public class SerializationUtil {
    public static Gson gson = new Gson();

    public static JsonElement ToJsonElement(ItemStack itemStack){
        return ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, itemStack).result().get();
    }

    public static ItemStack GetItemStack(JsonElement jsonElement){
        return ItemStack.CODEC.parse(JsonOps.INSTANCE, jsonElement).result().get();
    }

    public static JsonElement ToJsonElement(CompoundTag nbt){
        return CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, nbt).result().get();
    }

    public static CompoundTag GetNbt(JsonElement jsonElement){
        return CompoundTag.CODEC.parse(JsonOps.INSTANCE, jsonElement).result().get();
    }

    public static <T> JsonElement ToJsonElement(ResourceKey resourceKey, ResourceKey<? extends Registry<T>> registry){
        return (JsonElement) ResourceKey.codec(registry).encodeStart(JsonOps.INSTANCE, resourceKey).result().get();
    }

    public static <T> ResourceKey<T> GetResourceKey(JsonElement jsonElement, ResourceKey<? extends Registry<T>> registry){
        return ResourceKey.codec(registry).parse(JsonOps.INSTANCE, jsonElement).result().get();
    }

    public static String JsonElementToString(JsonElement jsonElement){
        return jsonElement.toString();
    }

    public static JsonElement StringToJsonElement(String string){
        return gson.fromJson(string, JsonElement.class);
    }
}
