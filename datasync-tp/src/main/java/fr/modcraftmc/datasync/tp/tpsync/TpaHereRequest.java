package fr.modcraftmc.datasync.tp.tpsync;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.tp.DatasyncTp;
import fr.modcraftmc.datasync.tp.message.TpaHereRequestMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class TpaHereRequest {
    private ISyncPlayer playerSource;
    private ISyncPlayer playerTarget;
    private int time;

    public TpaHereRequest(ISyncPlayer playerSource, ISyncPlayer playerTarget) {
        this(playerSource, playerTarget, (int) System.currentTimeMillis() / 1000);
    }

    public TpaHereRequest(ISyncPlayer playerSource, ISyncPlayer playerTarget, int time) {
        this.playerSource = playerSource;
        this.playerTarget = playerTarget;
        this.time = time;
    }

    public void fire() {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerSource.getUUID());
        if(player == null){
            DatasyncTp.LOGGER.warn("Trying to send a tpa here request from a player not on current server");
            return;
        }

        player.sendSystemMessage(Component.literal("Sending tpa here request to " + playerTarget));
        playerTarget.getServer().sendMessage(new TpaHereRequestMessage(this));
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
