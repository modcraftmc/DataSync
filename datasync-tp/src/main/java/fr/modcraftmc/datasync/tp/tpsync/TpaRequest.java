package fr.modcraftmc.datasync.tp.tpsync;

import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.tp.DatasyncTp;
import fr.modcraftmc.datasync.tp.message.TpaRequestMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class TpaRequest {
    private final ISyncPlayer playerSource;
    private final ISyncPlayer playerTarget;
    private final int time;

    public TpaRequest(ISyncPlayer playerSource, ISyncPlayer playerTarget) {
        this(playerSource, playerTarget, (int) System.currentTimeMillis() / 1000);
    }

    public TpaRequest(ISyncPlayer playerSource, ISyncPlayer playerTarget, int time) {
        this.playerSource = playerSource;
        this.playerTarget = playerTarget;
        this.time = time;
    }

    public void fire() {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerSource.getUUID());
        if(player == null){
            DatasyncTp.LOGGER.warn("Trying to send a tpa request from a player not on current server");
            return;
        }

        player.sendSystemMessage(Component.literal("Sending tpa request to " + playerTarget));
        playerTarget.getServer().sendMessage(new TpaRequestMessage(this));
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
