package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.tpsync.TpaRequest;
import fr.modcraftmc.datasync.tpsync.TpaRequestHandler;

public class TpaRequestMessage extends BaseMessage{
    public static final String MESSAGE_NAME = "TpaRequestMessage";

    public String playerSourceName;
    public String playerTargetName;
    public int time;

    public TpaRequestMessage(String playerSourceName, String playerTargetName, int time) {
        super(MESSAGE_NAME);
        this.playerSourceName = playerSourceName;
        this.playerTargetName = playerTargetName;
        this.time = time;
    }

    public TpaRequestMessage(TpaRequest tpaRequest) {
        this(tpaRequest.getPlayerSourceName(), tpaRequest.getPlayerTargetName(), tpaRequest.getTime());
    }

    public JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.addProperty("playerSourceName", playerSourceName);
        jsonObject.addProperty("playerTargetName", playerTargetName);
        jsonObject.addProperty("time", time);
        return jsonObject;
    }

    public static TpaRequestMessage deserialize(JsonObject json) {
        String playerSourceName = json.get("playerSourceName").getAsString();
        String playerTargetName = json.get("playerTargetName").getAsString();
        int time = json.get("time").getAsInt();
        return new TpaRequestMessage(playerSourceName, playerTargetName, time);
    }

    public TpaRequest getTpaRequest() {
        return new TpaRequest(playerSourceName, playerTargetName, time);
    }

    @Override
    protected void handle() {
        TpaRequestHandler.handle(getTpaRequest());
    }


}