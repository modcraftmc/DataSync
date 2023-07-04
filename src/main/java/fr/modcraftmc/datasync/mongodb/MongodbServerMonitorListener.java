package fr.modcraftmc.datasync.mongodb;

import com.mongodb.event.*;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.SecurityWatcher;

public class MongodbServerMonitorListener implements ServerMonitorListener {
    private boolean alive;

    public MongodbServerMonitorListener() {
        this.alive = true;
    }

    @Override
    public void serverHeartbeatSucceeded(ServerHeartbeatSucceededEvent event) {
        successHeartbeat();
    }

    @Override
    public void serverHeartbeatFailed(ServerHeartbeatFailedEvent event) {
        DataSync.LOGGER.error(String.format("Error on mongodb connection : %s", event.getThrowable().getMessage()));
        failHeartbeat();
    }

    private void successHeartbeat(){
        if(!alive){
            DataSync.LOGGER.warn("Mongodb server is back online");
            DataSync.dataSecurityWatcher.removeIssue(SecurityWatcher.MONGODB_CONNECTION_ISSUE);
            alive = true;
        }
    }

    private void failHeartbeat(){
        if(alive){
            DataSync.LOGGER.error("Mongodb server is unreachable");
            DataSync.dataSecurityWatcher.addIssue(SecurityWatcher.MONGODB_CONNECTION_ISSUE);
            alive = false;
        }
    }
}
