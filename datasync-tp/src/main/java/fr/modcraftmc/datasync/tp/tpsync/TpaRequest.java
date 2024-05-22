package fr.modcraftmc.datasync.tp.tpsync;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.tp.message.TpaRequestMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TpaRequest {
    private final ServerPlayer playerSource;
    private final String playerTargetName;
    private final int time;

    public TpaRequest(ServerPlayer playerSourceName, String playerTargetName) {
        this(playerSourceName, playerTargetName, (int) System.currentTimeMillis() / 1000);
    }

    public TpaRequest(ServerPlayer playerSource, String playerTargetName, int time) {
        this.playerSource = playerSource;
        this.playerTargetName = playerTargetName;
        this.time = time;
    }

    public void fire() {
        CrossServerCoreAPI.instance.findPlayer(playerTargetName).ifPresentOrElse(targetServer -> {
            playerSource.sendSystemMessage(Component.literal("Sending tpa request to " + playerTargetName));
            targetServer.sendMessage(new TpaRequestMessage(this).serializeToString());
        }, () -> playerSource.sendSystemMessage(Component.literal("Player " + playerTargetName + " not found")));
    }

    public String getPlayerSourceName() {
        return playerSource.getName().getString();
    }

    public String getPlayerTargetName() {
        return playerTargetName;
    }

    public int getTime() {
        return time;
    }
}
