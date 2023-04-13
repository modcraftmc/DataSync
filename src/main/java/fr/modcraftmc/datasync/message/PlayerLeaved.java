package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;

public class PlayerLeaved extends BaseMessage {
    public static final String MESSAGE_NAME = "player_leaved";

    private String playerName;
    private String serverName;

    public PlayerLeaved(String playerName, String serverName) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.serverName = serverName;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("playerName", playerName);
        jsonObject.addProperty("serverName", serverName);
        return jsonObject;
    }

    public static PlayerLeaved deserialize(JsonObject json) {
        String playerName = json.get("playerName").getAsString();
        String serverName = json.get("serverName").getAsString();
        return new PlayerLeaved(playerName, serverName);
    }

    @Override
    protected void handle() {
        DataSync.LOGGER.debug(String.format("Player %s leaved server %s", playerName, serverName));
        DataSync.playersLocation.removePlayer(playerName);
    }
}
