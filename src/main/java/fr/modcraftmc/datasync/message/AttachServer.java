package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.networkdiscovery.SyncServer;

public class AttachServer extends BaseMessage{
    public static final String MESSAGE_NAME = "AttachServer";
    public String serverName;

    public AttachServer(String serverName) {
        super(MESSAGE_NAME);
        this.serverName = serverName;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("serverName", serverName);
        return jsonObject;
    }

    public static AttachServer deserialize(JsonObject json) {
        String serverName = json.get("serverName").getAsString();
        return new AttachServer(serverName);
    }

    @Override
    protected void handle() {
        if(serverName.equals(DataSync.serverName)) return; // this message is send over all servers, we don't want to add ourself to the cluster
        DataSync.serverCluster.addServer(new SyncServer(serverName));
        DataSync.LOGGER.debug(String.format("Received attach request from %s and have been attached to the network", serverName));
        DataSync.serverCluster.getServer(serverName).get().sendMessage(new AttachServerResponse(DataSync.serverName, DataSync.serverCluster.getServer(DataSync.serverName).get().getPlayers()).serializeToString());
    }
}
