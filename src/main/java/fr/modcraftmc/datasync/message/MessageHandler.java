package fr.modcraftmc.datasync.message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.rabbitmq.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MessageHandler {
    static final Map<String, Function<JsonObject, ? extends BaseMessage>> messageMap = new HashMap<>();
    public static Gson GSON = new Gson();

    public static void init(){
        messageMap.put(TransferDataMessage.MESSAGE_NAME, TransferDataMessage::deserialize);
        messageMap.put(SaveToDBMessage.MESSAGE_NAME, SaveToDBMessage::deserialize);
        messageMap.put(LoadDataMessage.MESSAGE_NAME, LoadDataMessage::deserialize);
        messageMap.put(AttachServer.MESSAGE_NAME, AttachServer::deserialize);
        messageMap.put(AttachServerResponse.MESSAGE_NAME, AttachServerResponse::deserialize);
        messageMap.put(DetachServer.MESSAGE_NAME, DetachServer::deserialize);
        messageMap.put(PlayerJoined.MESSAGE_NAME, PlayerJoined::deserialize);
        messageMap.put(PlayerLeaved.MESSAGE_NAME, PlayerLeaved::deserialize);
        messageMap.put(TransferPlayer.MESSAGE_NAME, TransferPlayer::deserialize);
        messageMap.put(TpRequestMessage.MESSAGE_NAME, TpRequestMessage::deserialize);
        messageMap.put(SyncTeams.MESSAGE_NAME, SyncTeams::deserialize);
        messageMap.put(SyncTeamQuests.MESSAGE_NAME, SyncTeamQuests::deserialize);

        DataSync.onConfigLoad.add(() -> {
            RabbitmqDirectSubscriber.instance.subscribe(DataSync.serverName, (consumerTag, message) -> {
                DataSync.LOGGER.debug("Received message: " + new String(message.getBody()));
                String messageJson = new String(message.getBody(), StandardCharsets.UTF_8);
                MessageHandler.handle(messageJson);
            });

            RabbitmqSubscriber.instance.subscribe((consumerTag, message) -> {
                DataSync.LOGGER.debug("Received message: " + new String(message.getBody()));
                String messageJson = new String(message.getBody(), StandardCharsets.UTF_8);
                MessageHandler.handle(messageJson);
            });
        });
    }

    public static void handle(JsonObject message){
        if(messageMap.containsKey(message.get("messageName").getAsString()))
            messageMap.get(message.get("messageName").getAsString()).apply(message).handle();
        else
            DataSync.LOGGER.error("Message id not found");
    }

    public static void handle(String message){
        handle(GSON.fromJson(message, JsonObject.class));
    }
}
