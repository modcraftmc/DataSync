package fr.modcraftmc.datasync.tp.tpsync;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.tp.message.TpaRequestMessage;

public class TpaRequest {
    private final String playerSourceName;
    private final String playerTargetName;
    private final int time;

    public TpaRequest(String playerSourceName, String playerTargetName) {
        this(playerSourceName, playerTargetName, (int) System.currentTimeMillis() / 1000);
    }

    public TpaRequest(String playerSourceName, String playerTargetName, int time) {
        this.playerSourceName = playerSourceName;
        this.playerTargetName = playerTargetName;
        this.time = time;
    }

    public void fire() {
        CrossServerCoreAPI.instance.findPlayer(playerTargetName).ifPresent(targetServer -> {
            targetServer.sendMessage(new TpaRequestMessage(this).serializeToString());
        });
    }

    public String getPlayerSourceName() {
        return playerSourceName;
    }

    public String getPlayerTargetName() {
        return playerTargetName;
    }

    public int getTime() {
        return time;
    }
}
