package fr.modcraftmc.datasync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.modcraftmc.datasync.message.MessageHandler;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqConnection;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqDirectSubscriber;

import java.io.IOException;

public class RabbitmqEventSubscriber {
    private RabbitmqDirectSubscriber rabbitmqDirectSubscriber;

    public RabbitmqEventSubscriber(RabbitmqConnection rabbitmqConnection, String serverName) throws IOException {
        this.rabbitmqDirectSubscriber = new RabbitmqDirectSubscriber(rabbitmqConnection);

        rabbitmqDirectSubscriber.subscribe(serverName, (consumerTag, message) -> {
            DataSync.LOGGER.debug("Received message: " + new String(message.getBody()));
            String messageName = new String(message.getBody(), "UTF-8");
            Gson gson = new Gson();
            JsonObject jsonData = gson.fromJson(messageName, JsonObject.class);
            MessageHandler.handle(jsonData);
        });
    }
}
