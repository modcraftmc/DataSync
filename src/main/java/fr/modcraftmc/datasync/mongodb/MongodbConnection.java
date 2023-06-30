package fr.modcraftmc.datasync.mongodb;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MongodbConnection {
    MongoClientSettings settings;
    MongoClient client;

    public MongodbConnection(String host, int port, String username, String password, String authSource) {
        MongoCredential credential = MongoCredential.createCredential(username, authSource, password.toCharArray());
        settings = MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(List.of(new ServerAddress(host, port))))
                        .applyToSocketSettings(builder -> builder.connectTimeout(0, TimeUnit.SECONDS))
                        .applyToServerSettings(builder -> builder.addServerMonitorListener(new MongodbServerMonitorListener()))
                        .credential(credential)
                        .build();
        client = MongoClients.create(settings);
    }

    public MongoClient getClient() {
        return client;
    }

    public void close() {
        client.close();
    }
}
