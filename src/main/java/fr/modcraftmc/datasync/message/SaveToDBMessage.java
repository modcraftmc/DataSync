package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;

public class SaveToDBMessage extends BaseMessage<SaveToDBMessage> {
    public static final String MESSAGE_NAME = "SaveToDBMessage";
    private String playerName;
    private String serverName;

    public SaveToDBMessage(String playerName, String serverName) {
        super(MESSAGE_NAME, SaveToDBMessage::Deserialize);
        this.playerName = playerName;
        this.serverName = serverName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getServerName() {
        return serverName;
    }

    public JsonObject Serialize() {
        JsonObject jsonObject = super.Serialize();
        jsonObject.addProperty("serverName", serverName);
        jsonObject.addProperty("playerName", playerName);
        return jsonObject;
    }

    public static SaveToDBMessage Deserialize(JsonObject json) {
        String serverName = json.get("serverName").getAsString();
        String playerName = json.get("playerName").getAsString();
        return new SaveToDBMessage(playerName, serverName);
    }

    @Override
    protected void Handle() {
        // TODO: save player data to database
    }
}
