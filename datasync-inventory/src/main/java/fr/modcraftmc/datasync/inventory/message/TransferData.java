package fr.modcraftmc.datasync.inventory.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.inventory.PlayerDataSynchronizer;

public class TransferData extends BaseMessage {
    public static final String MESSAGE_NAME = "transfer_data";

    public final String playerName;
    public final JsonObject data;

    public TransferData(String playerName, JsonObject data) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.data = data;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("playerName", playerName);
        jsonObject.add("data", data);
        return jsonObject;
    }

    public static TransferData deserialize(JsonObject json) {
        String playerName = json.get("playerName").getAsString();
        JsonObject data = json.get("data").getAsJsonObject();
        return new TransferData(playerName, data);
    }

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    @Override
    public void handle() {
        PlayerDataSynchronizer.pushDataToTransferBuffer(playerName, data);
    }
}
