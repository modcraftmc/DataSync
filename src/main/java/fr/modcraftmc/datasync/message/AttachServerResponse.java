package fr.modcraftmc.datasync.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.networkidentity.SyncServer;

import java.util.ArrayList;
import java.util.List;

public class AttachServerResponse extends BaseMessage{
    public static final String MESSAGE_NAME = "AttachServerResponse";
    public String serverName;
    public List<String> players;

    AttachServerResponse(String serverName, List<String> players) {
        super(MESSAGE_NAME);
        this.serverName = serverName;
        this.players = players;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("serverName", serverName);
        JsonArray jsonArray = new JsonArray();
        for (String player : players) {
            jsonArray.add(player);
        }
        jsonObject.add("players", jsonArray);
        return jsonObject;
    }

    public static AttachServerResponse deserialize(JsonObject json) {
        String serverName = json.get("serverName").getAsString();
        JsonArray jsonArray = json.get("players").getAsJsonArray();
        List<String> players = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            players.add(jsonArray.get(i).getAsString());
        }
        return new AttachServerResponse(serverName, players);
    }

    @Override
    protected void handle() {
        SyncServer syncServer = new SyncServer(serverName);
        DataSync.serverCluster.addServer(syncServer);
        for (String player : players) {
            DataSync.playersLocation.setPlayerLocation(player, syncServer);
        }
        DataSync.LOGGER.debug("Server %s responded and have been attached to the network");
    }
}
