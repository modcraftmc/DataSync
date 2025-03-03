package fr.modcraftmc.datasync.waystones.message.autoserializer;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import fr.modcraftmc.crossservercore.api.message.autoserializer.FieldSerializer;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.core.WaystoneImpl;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.reflect.Type;

public class WaystoneSerialier extends FieldSerializer<Waystone> {
    @Override
    public JsonElement serialize(Waystone value) {
        HolderLookup.Provider lookup = ServerLifecycleHooks.getCurrentServer().registryAccess();
        CompoundTag tag = new CompoundTag();
        WaystoneImpl.write(value, tag, lookup);
        JsonElement waystoneJson = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, tag).result().get();
        return waystoneJson;
    }

    @Override
    public Waystone deserialize(JsonElement json, Type typeOfT) {
        HolderLookup.Provider lookup = ServerLifecycleHooks.getCurrentServer().registryAccess();
        CompoundTag waystoneTag = CompoundTag.CODEC.parse(JsonOps.INSTANCE, json).result().get();
        return WaystoneImpl.read(waystoneTag, lookup);
    }

    @Override
    public Type getType() {
        return Waystone.class;
    }
}
