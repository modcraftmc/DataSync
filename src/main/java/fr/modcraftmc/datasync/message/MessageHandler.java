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
        messageMap.put(TransferData.MESSAGE_NAME, TransferData::deserialize);
        messageMap.put(SaveToDBMessage.MESSAGE_NAME, SaveToDBMessage::deserialize);
        messageMap.put(LoadData.MESSAGE_NAME, LoadData::deserialize);
        messageMap.put(AttachServer.MESSAGE_NAME, AttachServer::deserialize);
        messageMap.put(AttachServerResponse.MESSAGE_NAME, AttachServerResponse::deserialize);
        messageMap.put(DetachServer.MESSAGE_NAME, DetachServer::deserialize);
        messageMap.put(PlayerJoined.MESSAGE_NAME, PlayerJoined::deserialize);
        messageMap.put(PlayerLeaved.MESSAGE_NAME, PlayerLeaved::deserialize);
        messageMap.put(TransferPlayer.MESSAGE_NAME, TransferPlayer::deserialize);
        messageMap.put(TpRequest.MESSAGE_NAME, TpRequest::deserialize);
        messageMap.put(TpaRequest.MESSAGE_NAME, TpaRequest::deserialize);
        messageMap.put(SyncTeams.MESSAGE_NAME, SyncTeams::deserialize);
        messageMap.put(SyncTeamQuests.MESSAGE_NAME, SyncTeamQuests::deserialize);
        messageMap.put(SyncTeamMessage.MESSAGE_NAME, SyncTeamMessage::deserialize);
        messageMap.put(SyncQuests.MESSAGE_NAME, SyncQuests::deserialize);
        messageMap.put(SendMessage.MESSAGE_NAME, SendMessage::deserialize);

        DataSync.onConfigLoad.add(() -> {
            RabbitmqDirectSubscriber.instance.subscribe(DataSync.serverName, (consumerTag, message) -> {
                DataSync.LOGGER.debug("Received message: " + new String(message.getBody()));
                String messageJson = new String(message.getBody(), StandardCharsets.UTF_8);
                try {
                    MessageHandler.handle(messageJson);
                } catch (Exception e) {
                    DataSync.LOGGER.error("Error while handling message", e);
                }
            });

            RabbitmqSubscriber.instance.subscribe((consumerTag, message) -> {
                DataSync.LOGGER.debug("Received message: " + new String(message.getBody()));
                String messageJson = new String(message.getBody(), StandardCharsets.UTF_8);
                try {
                    MessageHandler.handle(messageJson);
                } catch (Exception e) {
                    DataSync.LOGGER.error("Error while handling message", e);
                }
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
