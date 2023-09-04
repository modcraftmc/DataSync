package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.message.BaseMessage;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;

import java.util.UUID;

public class TeleportToWaystone extends BaseMessage {

    public static final String MESSAGE_NAME = "teleport_to_waystone";

    private String playerName;
    private UUID waystoneUUID;

    public TeleportToWaystone(String playerName, UUID waystoneUUID) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.waystoneUUID = waystoneUUID;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject object = super.serialize();
        object.addProperty("playerName", playerName);
        object.addProperty("WaystoneUUID", waystoneUUID.toString());
        return object;
    }

    public static TeleportToWaystone deserialize(JsonObject json) {
        return new TeleportToWaystone(json.get("playerName").getAsString(), UUID.fromString(json.get("WaystoneUUID").getAsString()));
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    protected void handle() {
        DatasyncWaystones.waystoneManager.addPendingWaystoneTp(playerName, waystoneUUID);
    }
}
