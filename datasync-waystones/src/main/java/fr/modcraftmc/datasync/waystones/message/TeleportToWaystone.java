package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.message.BaseMessage;
import fr.modcraftmc.datasync.waystones.WaystoneTpHandler;
import net.blay09.mods.waystones.api.IWaystone;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class TeleportToWaystone extends BaseMessage {

    public static final String MESSAGE_NAME = "teleport_to_waystone";

    private String player;
    private UUID waystone;

    public TeleportToWaystone(String serverPlayer, UUID waystone) {
        super(MESSAGE_NAME);
        this.player = serverPlayer;
        this.waystone = waystone;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject object = super.serialize();
        object.addProperty("name", player);
        object.addProperty("WaystoneUUID", waystone.toString());

        return object;
    }

    public static TeleportToWaystone deserialize(JsonObject json) {
        return new TeleportToWaystone(json.get("name").getAsString(), UUID.fromString(json.get("WaystoneUUID").getAsString()));
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    protected void handle() {
        WaystoneTpHandler.addTp(player, waystone);
    }
}
