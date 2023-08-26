package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;

public class PlayerJoined extends BaseMessage{
    public static final String MESSAGE_NAME = "PlayerJoined";

    public String playerName;
    public String serverName;

    public PlayerJoined(String playerName, String serverName) {
        super(MESSAGE_NAME);
        this.playerName = playerName;
        this.serverName = serverName;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("serverName", serverName);
        jsonObject.addProperty("playerName", playerName);
        return jsonObject;
    }

    public static PlayerJoined deserialize(JsonObject json) {
        String serverName = json.get("serverName").getAsString();
        String playerName = json.get("playerName").getAsString();
        return new PlayerJoined(playerName, serverName);
    }

    @Override
    protected void handle() {
        DataSync.serverCluster.getServer(serverName).ifPresentOrElse(
        syncServer -> {
            DataSync.LOGGER.debug(String.format("Player %s joined server %s", playerName, serverName));
            DataSync.playersLocation.setPlayerLocation(playerName, syncServer);
        },
        () -> {
            DataSync.playersLocation.removePlayer(playerName);
        });
    }
}
