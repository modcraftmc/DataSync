package fr.modcraftmc.datasync.tp.tpsync;

import fr.modcraftmc.crossservercoreapi.CrossServerCoreAPI;
import fr.modcraftmc.crossservercoreapi.CrossServerCoreProxyExtensionAPI;
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
        CrossServerCoreAPI.instance.findPlayer(playerTargetName).ifPresent(targetServer -> {
            CrossServerCoreAPI.instance.findPlayer(playerSourceName).ifPresent(sourceServer -> {
                if(!sourceServer.getName().equals(targetServer.getName()))
                    sendTransferPlayerProxyOrder(playerSourceName, targetServer.getName());
                targetServer.sendMessage(new TpRequestMessage(this).serializeToString());
            });
        });
    }

    private void sendTransferPlayerProxyOrder(String playerToTransfer, String serverName){
        CrossServerCoreProxyExtensionAPI.instance.transferPlayer(playerToTransfer, serverName);
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
