package fr.modcraftmc.datasync.tpsync;

import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.message.TpRequestMessage;
import fr.modcraftmc.datasync.message.TransferPlayer;

public class TpRequest {
    private final String playerSourceName;
    private final String playerTargetName;
    private final int time;

    public TpRequest(String playerSourceName, String playerTargetName) {
        this(playerSourceName, playerTargetName, (int) System.currentTimeMillis() / 1000);
    }

    public TpRequest(String playerSourceName, String playerTargetName, int time) {
        this.playerSourceName = playerSourceName;
        this.playerTargetName = playerTargetName;
        this.time = time;
    }

    public void fire() {
        DataSync.playersLocation.getPlayerLocation(playerTargetName).ifPresent(
                syncServer -> {
                    syncServer.sendMessage(new TpRequestMessage(this).serializeToString());
                    DataSync.sendProxy(new TransferPlayer(playerSourceName, syncServer.getName()).serializeToString());
                }
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
