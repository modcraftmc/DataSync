package fr.modcraftmc.datasync.networkidentity;

import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.message.MessageSender;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqDirectPublisher;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqPublisher;

import java.io.IOException;
import java.util.List;

public class SyncServer implements MessageSender {
    public final String serverName;


    public SyncServer(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public void sendMessage(String message) {
        try {
            RabbitmqDirectPublisher.instance.publish(serverName, message);
        } catch (IOException e) {
            DataSync.LOGGER.error(String.format("Error while publishing message to rabbitmq cannot send message to server %s : %s", serverName, e.getMessage()));
        }
    }
}
