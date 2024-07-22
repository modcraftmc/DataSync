package fr.modcraftmc.datasync.homes.messages;

import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.homes.DatasyncHomes;

import java.util.Optional;

@AutoRegister("change_player_homes_limit")
public class ChangePlayerHomesLimit extends BaseMessage {

    @AutoSerialize
    private ISyncPlayer player;
    @AutoSerialize
    private Optional<Integer> limit;

    private ChangePlayerHomesLimit() {}

    public ChangePlayerHomesLimit(ISyncPlayer player, Optional<Integer> limit) {
        this.player = player;
        this.limit = limit;
    }

    @Override
    public void handle() {
        if (limit.isPresent()) {
            DatasyncHomes.homeManager.setCachedPlayerHomesLimit(player, limit.get());
        } else {
            DatasyncHomes.homeManager.unsetCachedPlayerHomesLimit(player);
        }
    }
}
