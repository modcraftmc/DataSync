package fr.modcraftmc.datasync.inventory.serialization;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.TagParser;


public class SerializationUtil {
    public static String ToString(CompoundTag nbt){
        return new SnbtPrinterTagVisitor().visit(nbt);
    }

    public static CompoundTag ToNbt(String snbt){
        try {
            return TagParser.parseTag(snbt);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
