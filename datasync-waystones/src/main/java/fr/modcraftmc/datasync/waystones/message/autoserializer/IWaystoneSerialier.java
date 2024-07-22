package fr.modcraftmc.datasync.waystones.message.autoserializer;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import fr.modcraftmc.crossservercore.api.message.autoserializer.FieldSerializer;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.Waystone;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Type;

public class IWaystoneSerialier extends FieldSerializer<IWaystone> {
    @Override
    public JsonElement serialize(IWaystone value) {
        CompoundTag tag = new CompoundTag();
        Waystone.write(value, tag);
        JsonElement waystoneJson = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, tag).result().get();
        return waystoneJson;
    }

    @Override
    public IWaystone deserialize(JsonElement json, Type typeOfT) {
        CompoundTag waystoneTag = CompoundTag.CODEC.parse(JsonOps.INSTANCE, json).result().get();
        return Waystone.read(waystoneTag);
    }

    @Override
    public Type getType() {
        return IWaystone.class;
    }
}
