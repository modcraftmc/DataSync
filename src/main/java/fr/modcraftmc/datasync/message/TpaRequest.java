package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.tpsync.TpaRequestHandler;

public class TpaRequest extends BaseMessage{
    public static final String MESSAGE_NAME = "TpaRequestMessage";

    public String playerSourceName;
    public String playerTargetName;
    public int time;

    public TpaRequest(String playerSourceName, String playerTargetName, int time) {
        super(MESSAGE_NAME);
        this.playerSourceName = playerSourceName;
        this.playerTargetName = playerTargetName;
        this.time = time;
    }

    public TpaRequest(fr.modcraftmc.datasync.tpsync.TpaRequest tpaRequest) {
        this(tpaRequest.getPlayerSourceName(), tpaRequest.getPlayerTargetName(), tpaRequest.getTime());
    }

    public JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("playerSourceName", playerSourceName);
        jsonObject.addProperty("playerTargetName", playerTargetName);
        jsonObject.addProperty("time", time);
        return jsonObject;
    }

    public static TpaRequest deserialize(JsonObject json) {
        String playerSourceName = json.get("playerSourceName").getAsString();
        String playerTargetName = json.get("playerTargetName").getAsString();
        int time = json.get("time").getAsInt();
        return new TpaRequest(playerSourceName, playerTargetName, time);
    }

    public fr.modcraftmc.datasync.tpsync.TpaRequest getTpaRequest() {
        return new fr.modcraftmc.datasync.tpsync.TpaRequest(playerSourceName, playerTargetName, time);
    }

    @Override
    protected void handle() {
        TpaRequestHandler.handle(getTpaRequest());
    }


}
