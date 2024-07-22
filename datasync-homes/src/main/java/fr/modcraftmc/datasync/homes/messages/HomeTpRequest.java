package fr.modcraftmc.datasync.homes.messages;

import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.homes.DatasyncHomes;
import fr.modcraftmc.datasync.homes.HomeManager;

@AutoRegister("home_tp_request")
public class HomeTpRequest extends BaseMessage {

    @AutoSerialize
    private ISyncPlayer player;
    @AutoSerialize
    private HomeManager.Home home;

    private HomeTpRequest() {};

    public HomeTpRequest(ISyncPlayer player, HomeManager.Home home) {
        this.player = player;
        this.home = home;
    }

    @Override
    public void handle() {
        DatasyncHomes.homeManager.addPendingHomeTp(this);
    }

    public ISyncPlayer getPlayer() {
        return player;
    }

    public HomeManager.Home getHome() {
        return home;
    }
}
