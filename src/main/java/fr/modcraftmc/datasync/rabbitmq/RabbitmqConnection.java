package fr.modcraftmc.datasync.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RabbitmqConnection {
    private String host;
    private int port;
    private String username;
    private String password;
    private String virtualHost;

    private Connection connection;

    private List<Channel> channels = new ArrayList<Channel>();

    public RabbitmqConnection(String host, int port, String username, String password, String virtualHost) throws IOException, TimeoutException {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.virtualHost = virtualHost;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);

        connection = factory.newConnection();
    }

    public RabbitmqConnection(String host, String username, String password, String virtualHost) throws IOException, TimeoutException {
        this(host, 5672, username, password, virtualHost);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public Channel createChannel() {
        try {
            Channel channel = connection.createChannel();
            channels.add(channel);
            return channel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        for (Channel channel : channels) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            connection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
