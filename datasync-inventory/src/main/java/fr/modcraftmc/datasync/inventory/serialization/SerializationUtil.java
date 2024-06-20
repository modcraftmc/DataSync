package fr.modcraftmc.datasync.inventory.serialization;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;


public class SerializationUtil {
    public static String ToString(CompoundTag nbt){
        return NbtUtils.structureToSnbt(nbt);
    }

    public static CompoundTag ToNbt(String snbt){
        try {
            return NbtUtils.snbtToStructure(snbt);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
