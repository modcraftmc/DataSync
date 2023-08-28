package fr.modcraftmc.datasync.tp.tpsync;

import fr.modcraftmc.crossservercore.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.CrossServerCoreProxyExtensionAPI;
import fr.modcraftmc.datasync.tp.message.TpRequestMessage;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

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
        CrossServerCoreAPI.findPlayer(playerTargetName).ifPresent(targetServer -> {
            if(!targetServer.getName().equals(CrossServerCoreAPI.getServerName()))
                sendTransferPlayerProxyOrder(targetServer.getName());
            targetServer.sendMessage(new TpRequestMessage(this).serializeToString());
        });
    }

    private void sendTransferPlayerProxyOrder(String serverName){
        CrossServerCoreProxyExtensionAPI.transferPlayer(playerSourceName, serverName);
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
