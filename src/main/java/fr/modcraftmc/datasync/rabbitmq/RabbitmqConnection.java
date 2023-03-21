package fr.modcraftmc.datasync.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import fr.modcraftmc.datasync.DataSync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RabbitmqConnection {
    private final Connection connection;

    private final List<Channel> channels = new ArrayList<>();

    public RabbitmqConnection(String host, int port, String username, String password, String virtualHost) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);

        try {
            connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            DataSync.LOGGER.error("Error while connecting to RabbitMQ : %s".formatted(e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public RabbitmqConnection(String host, String username, String password, String virtualHost) throws IOException, TimeoutException {
        this(host, 5672, username, password, virtualHost);
    }

    public Channel createChannel() {
        try {
            Channel channel = connection.createChannel();
            channels.add(channel);
            return channel;
        } catch (IOException e) {
            DataSync.LOGGER.error(String.format("Error while creating rabbitmq channel %s", e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public void close() {
        for (Channel channel : channels) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                DataSync.LOGGER.error(String.format("Error while closing rabbitmq channel %s", e.getMessage()));
                throw new RuntimeException(e);
            }
        }
        try {
            connection.close();
        } catch (IOException e) {
            DataSync.LOGGER.error(String.format("Error while closing rabbitmq connection %s", e.getMessage()));
            throw new RuntimeException(e);
        }
    }
}
