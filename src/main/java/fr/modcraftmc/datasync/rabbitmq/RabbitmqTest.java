package fr.modcraftmc.datasync.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import fr.modcraftmc.datasync.DataSync;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class RabbitmqTest {
    private final static String IP_ADDRESS = "172.17.225.216";
    private final static String VHOST_NAME = "datasyncq";
    private final static String USERNAME = "datasync";
    private final static String PASSWORD = "datasync";
    private final static String QUEUE_NAME = "DataSync";

    public static void sendMessage(){
        ConnectionFactory connectionFactory = new ConnectionFactory();

        connectionFactory.setHost(IP_ADDRESS);
        connectionFactory.setVirtualHost(VHOST_NAME);
        connectionFactory.setUsername(USERNAME);
        connectionFactory.setPassword(PASSWORD);
        try (
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
        ){
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            String message = "Hello World!";
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            DataSync.LOGGER.info(String.format("Message {%s} sent", message));
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public static void prepareReceiveMessage(){
        ConnectionFactory connectionFactory = new ConnectionFactory();

        connectionFactory.setHost(IP_ADDRESS);
        connectionFactory.setVirtualHost(VHOST_NAME);
        connectionFactory.setUsername(USERNAME);
        connectionFactory.setPassword(PASSWORD);
        try (
                Connection connection = connectionFactory.newConnection();
                Channel channel = connection.createChannel();
        ){
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                DataSync.LOGGER.info(String.format("Received message {%s}", message));
            };
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
