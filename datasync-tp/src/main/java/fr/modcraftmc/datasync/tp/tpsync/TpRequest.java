package fr.modcraftmc.datasync.tp.tpsync;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.CrossServerCoreProxyExtensionAPI;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.datasync.tp.message.TpRequestMessage;

public class TpRequest {
    private final ISyncPlayer playerSource;
    private final ISyncPlayer playerTarget;
    private final int time;

    public TpRequest(ISyncPlayer playerSource, ISyncPlayer playerTarget) {
        this(playerSource, playerTarget, (int) System.currentTimeMillis() / 1000);
    }

    public TpRequest(ISyncPlayer playerSource, ISyncPlayer playerTarget, int time) {
        this.playerSource = playerSource;
        this.playerTarget = playerTarget;
        this.time = time;
    }

    public void fire() {
        ISyncServer targetServer = playerTarget.getServer();

        if(!playerSource.getServer().equals(targetServer))
            sendTransferPlayerProxyOrder(playerSource, targetServer);
        targetServer.sendMessage(new TpRequestMessage(this));
    }

    private void sendTransferPlayerProxyOrder(ISyncPlayer player, ISyncServer server){
        CrossServerCoreProxyExtensionAPI.transferPlayer(player, server);
    }

    public ISyncPlayer getPlayerSource() {
        return playerSource;
    }

    public ISyncPlayer getPlayerTarget() {
        return playerTarget;
    }

    public int getTime() {
        return time;
    }
}
