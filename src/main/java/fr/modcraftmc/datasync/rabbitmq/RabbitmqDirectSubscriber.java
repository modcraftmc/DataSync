package fr.modcraftmc.datasync.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;

public class RabbitmqDirectSubscriber {
    private final String EXCHANGE_NAME = "direct_events";

    private Channel rabbitmqChannel;

    public RabbitmqDirectSubscriber(RabbitmqConnection rabbitmqConnection) throws IOException {
        this.rabbitmqChannel = rabbitmqConnection.createChannel();
        rabbitmqChannel.exchangeDeclare(EXCHANGE_NAME, "direct");
    }

    public void subscribe(String routingKey, DeliverCallback listener) throws IOException {
        String queueName = rabbitmqChannel.queueDeclare().getQueue();
        rabbitmqChannel.queueBind(queueName, EXCHANGE_NAME, routingKey);
        rabbitmqChannel.basicConsume(queueName, true, listener, consumerTag -> {});
    }
}
