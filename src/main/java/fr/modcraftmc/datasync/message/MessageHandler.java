package fr.modcraftmc.datasync.message;

import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.DataSync;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MessageHandler {
    static final Map<String, Function<JsonObject, ? extends BaseMessage>> messageMap = new HashMap<>();

    public static void init(){
        messageMap.put(TransferMessage.MESSAGE_NAME, TransferMessage::Deserialize);
        messageMap.put(SaveToDBMessage.MESSAGE_NAME, SaveToDBMessage::Deserialize);
    }

    public static void handle(JsonObject message){
        if(messageMap.containsKey(message.get("messageName").getAsString()))
            messageMap.get(message.get("messageName").getAsString()).apply(message).Handle();
        else
            DataSync.LOGGER.error("Message id not found");
    }
}
