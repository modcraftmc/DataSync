package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import fr.modcraftmc.crossservercoreapi.message.BaseMessage;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.Waystone;
import net.blay09.mods.waystones.core.WaystoneManager;
import net.blay09.mods.waystones.core.WaystoneSyncManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

public class UpdateWaystone extends BaseMessage {

    public static final String MESSAGE_NAME = "update_waystones";

    private IWaystone iwaystone;

    public UpdateWaystone(IWaystone waystone) {
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

    public static UpdateWaystone deserialize(JsonObject json) {
        CompoundTag waystoneTag = CompoundTag.CODEC.parse(JsonOps.INSTANCE, json.get("waystone")).result().get();
        return new UpdateWaystone(Waystone.read(waystoneTag));
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    public void handle() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        WaystoneManager.get(server).addWaystone(iwaystone);

        DatasyncWaystones.LOGGER.info("updating waystones");

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            WaystoneSyncManager.sendWaystoneUpdate(player, iwaystone);
            WaystoneSyncManager.sendActivatedWaystones(player);
        }
    }
}
