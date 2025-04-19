package fr.modcraftmc.datasync.inventory.serialization;

import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import fr.modcraftmc.datasync.inventory.DatasyncInventory;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.server.ServerLifecycleHooks;


public class SerializationUtil {


    public static String ToString(CompoundTag nbt){
        DataResult<JsonElement> dataResult = CompoundTag.CODEC.encodeStart(ServerLifecycleHooks.getCurrentServer().registryAccess().createSerializationContext(JsonOps.INSTANCE), nbt);

        if (dataResult.error().isPresent()) {
            DatasyncInventory.LOGGER.error("Failed to serialize NBT: " + dataResult.error().get().message());
            return null; // TODO: better error handling
        }
        return dataResult.getOrThrow().toString();
    }

    public static CompoundTag ToNbt(String snbt){
        JsonElement jsonElement = new JsonParser().parse(snbt);

        DataResult<CompoundTag> dataResult = CompoundTag.CODEC.parse(ServerLifecycleHooks.getCurrentServer().registryAccess().createSerializationContext(JsonOps.INSTANCE), jsonElement);

        if (dataResult.error().isPresent()) {
            DatasyncInventory.LOGGER.error("Failed to parse SNBT: " + dataResult.error().get().message());
            return null; // TODO: better error handling
        }
        return dataResult.getPartialOrThrow();
    }
}
