package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;

public class TransferMessage extends BaseMessage<TransferMessage> {
    public static final String MESSAGE_NAME = "TransferMessage";
    private String playerName;
    private String oldServerName;
    private String newServerName;

    public TransferMessage(String playerName, String oldServerName, String newServerName) {
        super(MESSAGE_NAME, TransferMessage::Deserialize);
        this.playerName = playerName;
        this.oldServerName = oldServerName;
        this.newServerName = newServerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getOldServerName() {
        return oldServerName;
    }

    public String getNewServerName() {
        return newServerName;
    }

    public JsonObject Serialize() {
        JsonObject jsonObject = super.Serialize();
        jsonObject.addProperty("oldServerName", oldServerName);
        jsonObject.addProperty("newServerName", newServerName);
        jsonObject.addProperty("playerName", playerName);
        return jsonObject;
    }

    @Override
    protected void Handle() {
        //TODO: handle transfer
    }

    protected static TransferMessage Deserialize(JsonObject json) {
        String oldServerName = json.get("oldServerName").getAsString();
        String newServerName = json.get("newServerName").getAsString();
        String playerName = json.get("playerName").getAsString();
        return new TransferMessage(playerName, oldServerName, newServerName);
    }
}
