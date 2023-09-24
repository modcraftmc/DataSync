package fr.modcraftmc.datasync.ftbquests.Serialization;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;


public class SerializationUtil {
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
}
