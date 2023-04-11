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
        messageMap.put(TransferMessage.MESSAGE_NAME, TransferMessage::Deserialize);
        messageMap.put(SaveToDBMessage.MESSAGE_NAME, SaveToDBMessage::Deserialize);
        messageMap.put(LoadDataMessage.MESSAGE_NAME, LoadDataMessage::Deserialize);

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
            messageMap.get(message.get("messageName").getAsString()).apply(message).Handle();
        else
            DataSync.LOGGER.error("Message id not found");
    }

    public static void handle(String message){
        handle(GSON.fromJson(message, JsonObject.class));
    }
}
