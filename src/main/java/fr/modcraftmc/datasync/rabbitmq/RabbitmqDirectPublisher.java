package fr.modcraftmc.datasync.rabbitmq;

import com.rabbitmq.client.Channel;
import fr.modcraftmc.datasync.DataSync;

import java.io.IOException;

public class RabbitmqDirectPublisher {
    private final String EXCHANGE_NAME = "direct_events";
    public static RabbitmqDirectPublisher instance;

    private final Channel rabbitmqChannel;

    public RabbitmqDirectPublisher(RabbitmqConnection rabbitmqConnection) {
        this.rabbitmqChannel = rabbitmqConnection.createChannel();
        try {
            rabbitmqChannel.exchangeDeclare(EXCHANGE_NAME, "direct");
        } catch (IOException e) {
            DataSync.LOGGER.error("Error while creating RabbitMQ exchange");
            throw new RuntimeException(e);
        }
        instance = this;
    }

    public void publish(String routingKey, String message) throws IOException {
        rabbitmqChannel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes());
    }
}
