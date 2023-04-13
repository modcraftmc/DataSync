package fr.modcraftmc.datasync.rabbitmq;

import com.rabbitmq.client.Channel;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.References;

import java.io.IOException;

public class RabbitmqDirectPublisher {
    public static RabbitmqDirectPublisher instance;

    private final Channel rabbitmqChannel;

    public RabbitmqDirectPublisher(RabbitmqConnection rabbitmqConnection) {
        this.rabbitmqChannel = rabbitmqConnection.createChannel();
        try {
            rabbitmqChannel.exchangeDeclare(References.DIRECT_EXCHANGE_NAME, "direct");
        } catch (IOException e) {
            DataSync.LOGGER.error("Error while creating RabbitMQ exchange");
            throw new RuntimeException(e);
        }
        instance = this;
    }

    public void publish(String routingKey, String message) throws IOException {
        DataSync.LOGGER.debug(String.format("Publishing message to %s with routing key %s", References.DIRECT_EXCHANGE_NAME, routingKey));
        rabbitmqChannel.basicPublish(References.DIRECT_EXCHANGE_NAME, routingKey, null, message.getBytes());
    }
}
