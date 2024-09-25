package fr.modcraftmc.datasync.lightning.message.autoserializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.message.autoserializer.FieldSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistry;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class RegistrySnapshotSerializer extends FieldSerializer<ForgeRegistry.Snapshot> {
    @Override
    public JsonElement serialize(ForgeRegistry.Snapshot value) {
        CompoundTag tag = value.write();
        return CrossServerCoreAPI.getMessageAutoPropertySerializer().serializeObject(tag);
    }

    @Override
    public ForgeRegistry.Snapshot deserialize(JsonElement json, Type typeOfT) {
        CompoundTag tag = CrossServerCoreAPI.getMessageAutoPropertySerializer().deserializeObject(json, CompoundTag.class);
        return ForgeRegistry.Snapshot.read(tag);
    }

    @Override
    public Type getType() {
        return ForgeRegistry.Snapshot.class;
    }
}
