package fr.modcraftmc.datasync.tpsync;

import fr.modcraftmc.datasync.DataSync;

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
        DataSync.playersLocation.getPlayerLocation(playerTargetName).ifPresent(
                syncServer -> syncServer.sendMessage(new fr.modcraftmc.datasync.message.TpaRequest(this).serializeToString())
        );
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
