package fr.modcraftmc.datasync.lightning.message.autoserializer;

import com.google.gson.JsonElement;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.message.autoserializer.FieldSerializer;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.registries.RegistrySnapshot;

import java.lang.reflect.Type;

public class RegistrySnapshotSerializer extends FieldSerializer<RegistrySnapshot> {
    @Override
    public JsonElement serialize(RegistrySnapshot value) {
//        CompoundTag tag = value.write();
        CompoundTag tag = new CompoundTag();
        return CrossServerCoreAPI.getMessageAutoPropertySerializer().serializeObject(tag);
    }

    @Override
    public RegistrySnapshot deserialize(JsonElement json, Type typeOfT) {
        CompoundTag tag = CrossServerCoreAPI.getMessageAutoPropertySerializer().deserializeObject(json, CompoundTag.class);
//        return RegistrySnapshot.read(tag);
        return null;
    }

    @Override
    public Type getType() {
        return RegistrySnapshot.class;
    }
}
