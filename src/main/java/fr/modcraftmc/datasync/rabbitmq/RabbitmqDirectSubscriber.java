package fr.modcraftmc.datasync.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.References;
import fr.modcraftmc.shared.rabbitmq.RabbitmqConnection;

import java.io.IOException;

public class RabbitmqDirectSubscriber {
    public static RabbitmqDirectSubscriber instance;

    private final Channel rabbitmqChannel;

    public RabbitmqDirectSubscriber(RabbitmqConnection rabbitmqConnection) {
        try {
            this.rabbitmqChannel = rabbitmqConnection.createChannel();
            rabbitmqChannel.exchangeDeclare(References.DIRECT_EXCHANGE_NAME, "direct");
        } catch (IOException e) {
            DataSync.LOGGER.error("Error while creating RabbitMQ exchange");
            throw new RuntimeException(e);
        }
        instance = this;
    }

    public void subscribe(String routingKey, DeliverCallback listener) {
        String queueName = null;
        try {
            queueName = rabbitmqChannel.queueDeclare().getQueue();
            rabbitmqChannel.queueBind(queueName, References.DIRECT_EXCHANGE_NAME, routingKey);
            rabbitmqChannel.basicConsume(queueName, true, listener, consumerTag -> {});
        } catch (IOException e) {
            DataSync.LOGGER.error("Error while subscribing to RabbitMQ exchange");
            throw new RuntimeException(e);
        }
    }
}
