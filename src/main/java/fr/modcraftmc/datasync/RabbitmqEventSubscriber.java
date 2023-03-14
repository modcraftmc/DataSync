package fr.modcraftmc.datasync;

import fr.modcraftmc.datasync.rabbitmq.RabbitmqConnection;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqDirectSubscriber;

import java.io.IOException;

public class RabbitmqEventSubscriber {
    private RabbitmqDirectSubscriber rabbitmqDirectSubscriber;

    public RabbitmqEventSubscriber(RabbitmqConnection rabbitmqConnection, String serverName) throws IOException {
        this.rabbitmqDirectSubscriber = new RabbitmqDirectSubscriber(rabbitmqConnection);

        rabbitmqDirectSubscriber.subscribe(serverName, (consumerTag, message) -> {
            System.out.println("Received message: " + new String(message.getBody()));
        });
    }
}
