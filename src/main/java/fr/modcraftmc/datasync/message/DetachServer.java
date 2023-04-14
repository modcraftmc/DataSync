package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;

public class DetachServer extends BaseMessage{
    public static final String MESSAGE_NAME = "Detach";

    public String serverName;


    public DetachServer(String serverName) {
        super(MESSAGE_NAME);
        this.serverName = serverName;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("serverName", serverName);
        return jsonObject;
    }

    public static DetachServer deserialize(JsonObject json) {
        String serverName = json.get("serverName").getAsString();
        return new DetachServer(serverName);
    }

    @Override
    protected void handle() {
        DataSync.serverCluster.removeServer(serverName);
        DataSync.LOGGER.debug(String.format("Server %s have been detached from the network", serverName));
    }
}
