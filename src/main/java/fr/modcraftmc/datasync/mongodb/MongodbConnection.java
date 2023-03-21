package fr.modcraftmc.datasync.mongodb;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.util.List;

public class MongodbConnection {
    MongoClientSettings settings;
    MongoClient client;

    public MongodbConnection(String host, int port, String username, String password, String authSource) {
        MongoCredential credential = MongoCredential.createCredential(username, authSource, password.toCharArray());
        settings = MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(List.of(new ServerAddress(host, port))))
                        .credential(credential)
                        .build();
        client = MongoClients.create(settings);
    }

    public MongoClient getClient() {
        return client;
    }
}
