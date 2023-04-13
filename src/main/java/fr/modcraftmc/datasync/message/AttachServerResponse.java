package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.networkidentity.SyncServer;

public class AttachServerResponse extends BaseMessage{
    public static final String MESSAGE_NAME = "AttachServerResponse";
    public String serverName;

    AttachServerResponse(String serverName) {
        super(MESSAGE_NAME);
        this.serverName = serverName;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("serverName", serverName);
        return jsonObject;
    }

    public static AttachServerResponse deserialize(JsonObject json) {
        String serverName = json.get("serverName").getAsString();
        return new AttachServerResponse(serverName);
    }

    @Override
    protected void handle() {
        DataSync.serverCluster.addServer(new SyncServer(serverName));
        DataSync.LOGGER.debug("Server %s responded and have been attached to the network");
    }
}
