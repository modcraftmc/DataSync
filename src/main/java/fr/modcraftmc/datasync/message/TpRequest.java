package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.tpsync.TpRequestHandler;

public class TpRequest extends BaseMessage {
    public static final String MESSAGE_NAME = "TpRequestMessage";
    public String playerSourceName;
    public String playerTargetName;
    public int time;

    public TpRequest(fr.modcraftmc.datasync.tpsync.TpRequest tpRequest) {
        this(tpRequest.getPlayerSourceName(), tpRequest.getPlayerTargetName(), tpRequest.getTime());
    }

    public TpRequest(String playerSourceName, String playerTargetName, int time) {
        super(MESSAGE_NAME);
        this.playerSourceName = playerSourceName;
        this.playerTargetName = playerTargetName;
        this.time = time;
    }

    @Override
    protected JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("playerSourceName", playerSourceName);
        jsonObject.addProperty("playerTargetName", playerTargetName);
        jsonObject.addProperty("time", time);
        return jsonObject;
    }

    public static TpRequest deserialize(JsonObject json) {
        String playerSourceName = json.get("playerSourceName").getAsString();
        String playerTargetName = json.get("playerTargetName").getAsString();
        int time = json.get("time").getAsInt();
        return new TpRequest(playerSourceName, playerTargetName, time);
    }

    public fr.modcraftmc.datasync.tpsync.TpRequest getTpRequest() {
        return new fr.modcraftmc.datasync.tpsync.TpRequest(playerSourceName, playerTargetName, time);
    }

    @Override
    protected void handle() {
        TpRequestHandler.handleTpRequest(getTpRequest());
    }
}
