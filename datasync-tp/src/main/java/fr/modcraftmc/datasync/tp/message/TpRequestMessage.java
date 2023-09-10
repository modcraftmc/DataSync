package fr.modcraftmc.datasync.tp.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercoreapi.message.BaseMessage;
import fr.modcraftmc.datasync.tp.tpsync.TpRequest;
import fr.modcraftmc.datasync.tp.tpsync.TpRequestHandler;

public class TpRequestMessage extends BaseMessage {
    public static final String MESSAGE_NAME = "tp_request_message";
    public String playerSourceName;
    public String playerTargetName;
    public int time;

    public TpRequestMessage(TpRequest tpRequest) {
        this(tpRequest.getPlayerSourceName(), tpRequest.getPlayerTargetName(), tpRequest.getTime());
    }

    public TpRequestMessage(String playerSourceName, String playerTargetName, int time) {
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

    @Override
    public String getMessageName() {
        return MESSAGE_NAME;
    }

    public static TpRequestMessage deserialize(JsonObject json) {
        String playerSourceName = json.get("playerSourceName").getAsString();
        String playerTargetName = json.get("playerTargetName").getAsString();
        int time = json.get("time").getAsInt();
        return new TpRequestMessage(playerSourceName, playerTargetName, time);
    }

    @Override
    public void handle() {
        TpRequestHandler.handle(new TpRequest(playerSourceName, playerTargetName, time));
    }
}
