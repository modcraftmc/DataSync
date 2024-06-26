package fr.modcraftmc.datasync.tp.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.datasync.tp.tpsync.TpaHereRequest;
import fr.modcraftmc.datasync.tp.tpsync.TpaHereRequestHandler;
import fr.modcraftmc.datasync.tp.tpsync.TpaRequest;
import fr.modcraftmc.datasync.tp.tpsync.TpaRequestHandler;
import net.minecraftforge.server.ServerLifecycleHooks;

public class TpaHereRequestMessage extends BaseMessage {
    public static final String MESSAGE_NAME = "tpa_here_request_message";

    public String playerSourceName;
    public String playerTargetName;
    public int time;

    public TpaHereRequestMessage(String playerSourceName, String playerTargetName, int time) {
        super(MESSAGE_NAME);
        this.playerSourceName = playerSourceName;
        this.playerTargetName = playerTargetName;
        this.time = time;
    }

    public TpaHereRequestMessage(TpaHereRequest tpaRequest) {
        this(tpaRequest.getPlayerSourceName(), tpaRequest.getPlayerTargetName(), tpaRequest.getTime());
    }

    public JsonObject serialize() {
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

    public static TpaHereRequestMessage deserialize(JsonObject json) {
        String playerSourceName = json.get("playerSourceName").getAsString();
        String playerTargetName = json.get("playerTargetName").getAsString();
        int time = json.get("time").getAsInt();
        return new TpaHereRequestMessage(playerSourceName, playerTargetName, time);
    }

    public TpaHereRequest getTpaHereRequest() {
        return new TpaHereRequest(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerSourceName), playerTargetName, time);
    }

    @Override
    public void handle() {
        TpaHereRequestHandler.handle(getTpaHereRequest());
    }


}
