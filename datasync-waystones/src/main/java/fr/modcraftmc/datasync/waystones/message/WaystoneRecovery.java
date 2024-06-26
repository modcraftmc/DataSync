package fr.modcraftmc.datasync.waystones.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;

import java.util.UUID;

public class WaystoneRecovery extends BaseMessage {
    public static final String MESSAGE_NAME = "waystone_recovery";

    private final UUID waystoneUUID;

    public WaystoneRecovery(UUID waystoneUUID) {
        super(MESSAGE_NAME);
        this.waystoneUUID = waystoneUUID;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("waystoneUUID", waystoneUUID.toString());
        return jsonObject;
    }

    public static WaystoneRecovery deserialize(JsonObject json) {
        return new WaystoneRecovery(UUID.fromString(json.get("waystoneUUID").getAsString()));
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    public void handle() {
        DatasyncWaystones.waystoneManager.onRecoveryAsked(waystoneUUID);
    }
}
