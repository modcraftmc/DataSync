package fr.modcraftmc.datasync.waystones.message;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import net.blay09.mods.waystones.api.Waystone;

@AutoRegister("update_waystones")
public class UpdateWaystone extends BaseMessage {

    @AutoSerialize
    private Waystone waystone;
    @AutoSerialize
    private ISyncServer server;

    private UpdateWaystone() {}

    public UpdateWaystone(Waystone waystone) {
        this(waystone, CrossServerCoreAPI.getServer());
    }

    private UpdateWaystone(Waystone waystone, ISyncServer server) {
        this.waystone = waystone;
        this.server = server;
    }

    @Override
    public void handle() {
        DatasyncWaystones.waystoneManager.enableWaystone(waystone, server);
    }
}
