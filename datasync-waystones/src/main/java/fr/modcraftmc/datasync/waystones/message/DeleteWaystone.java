package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.Waystone;
import net.blay09.mods.waystones.core.WaystoneManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class DeleteWaystone extends BaseMessage {
    public static final String MESSAGE_NAME = "delete_waystone";

    private IWaystone iwaystone;

    public DeleteWaystone(IWaystone waystone) {
        super(MESSAGE_NAME);
        this.iwaystone = waystone;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject object = super.serialize();
        CompoundTag tag = new CompoundTag();
        Waystone.write(iwaystone, tag);
        JsonElement waystoneJson = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, tag).result().get();
        object.add("waystone", waystoneJson);

        return object;
    }

    public static DeleteWaystone deserialize(JsonObject json) {
        CompoundTag waystoneTag = CompoundTag.CODEC.parse(JsonOps.INSTANCE, json.get("waystone")).result().get();
        return new DeleteWaystone(Waystone.read(waystoneTag));
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    public void handle() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        WaystoneManager.get(server).removeWaystone(iwaystone);
        PlayerWaystoneManager.removeKnownWaystone(server, iwaystone);
    }
}
