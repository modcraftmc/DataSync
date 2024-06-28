package fr.modcraftmc.datasync.tp.tpsync;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.tp.DatasyncTp;
import fr.modcraftmc.datasync.tp.message.TpaRequestMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

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
        ServerPlayer playerSource = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerSourceName);
        if(playerSource == null){
            DatasyncTp.LOGGER.warn("Trying to send a tpa request from a player not on current server");
            return;
        }

        CrossServerCoreAPI.instance.findPlayer(playerTargetName).ifPresentOrElse(targetServer -> {
            playerSource.sendSystemMessage(Component.literal("Sending tpa request to " + playerTargetName));
            targetServer.sendMessage(new TpaRequestMessage(this).serializeToString());
        }, () -> playerSource.sendSystemMessage(Component.literal("Player " + playerTargetName + " not found")));
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
