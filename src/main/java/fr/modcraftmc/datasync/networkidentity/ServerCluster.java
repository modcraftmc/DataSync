package fr.modcraftmc.datasync.networkidentity;

import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.message.AttachServer;
import fr.modcraftmc.datasync.message.DetachServer;
import fr.modcraftmc.datasync.message.MessageSender;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqPublisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerCluster implements MessageSender {
    public List<SyncServer> servers;

    public ServerCluster() {
        this.servers = new ArrayList<>();
    }

    public void addServer(SyncServer server){
        servers.add(server);
    }

    public SyncServer getServer(String serverName){
        for (SyncServer server : servers) {
            if(server.serverName.equals(serverName)) return server;
        }
        DataSync.LOGGER.error(String.format("Server %s not found in cluster", serverName));
        return null;
    }

    public void attach(){
        addServer(new SyncServer(DataSync.serverName));
        try {
            DataSync.LOGGER.debug("Attaching to server cluster");
            RabbitmqPublisher.instance.publish(new AttachServer(DataSync.serverName).serializeToString());
        } catch (IOException e) {
            DataSync.LOGGER.error(String.format("Error while publishing message to rabbitmq cannot attach to server cluster : %s", e.getMessage()));
        }
    }

    public void detach(){
        removeServer(DataSync.serverName);
        try {
            DataSync.LOGGER.debug("Detaching from server cluster");
            RabbitmqPublisher.instance.publish(new DetachServer(DataSync.serverName).serializeToString());
        } catch (IOException e) {
            DataSync.LOGGER.error(String.format("Error while publishing message to rabbitmq cannot detach from server cluster : %s", e.getMessage()));
        }
    }

    @Override
    public void sendMessage(String message) {
        for (SyncServer server : servers) {
            server.sendMessage(message);
        }
    }

    public void removeServer(String serverName) {
        servers.removeIf(server -> server.serverName.equals(serverName));
    }
}
