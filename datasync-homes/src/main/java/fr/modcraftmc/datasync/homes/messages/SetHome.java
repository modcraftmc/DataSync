package fr.modcraftmc.datasync.homes.messages;

import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.homes.DatasyncHomes;
import fr.modcraftmc.datasync.homes.HomeManager;

@AutoRegister("set_home")
public class SetHome extends BaseMessage {

    public enum ActionType {
        SET,
        DELETE
    }

    @AutoSerialize
    public final String actionType;
    @AutoSerialize
    public final HomeManager.Home home;
    @AutoSerialize
    public final ISyncPlayer player;

    public SetHome(ISyncPlayer player, ActionType actionType, HomeManager.Home home) {
        this.player = player;
        this.actionType = actionType.name();
        this.home = home;
    }

    @Override
    public void handle() {
        ActionType action = ActionType.valueOf(this.actionType);
        switch (action) {
            case SET -> DatasyncHomes.homeManager.addCachedHome(player, home);
            case DELETE -> DatasyncHomes.homeManager.removeCachedHome(player, home.name());
        }
    }
}
