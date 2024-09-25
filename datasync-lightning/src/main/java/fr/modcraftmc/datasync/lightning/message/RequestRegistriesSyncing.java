package fr.modcraftmc.datasync.lightning.message;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.datasync.lightning.DatasyncLightning;

@AutoRegister("request_registries_syncing")
public class RequestRegistriesSyncing extends BaseMessage {

    @AutoSerialize
    private ISyncServer serverRequesting;

    public RequestRegistriesSyncing() {
        serverRequesting = CrossServerCoreAPI.getServer();
    }

    @Override
    public void handle() {
        if(DatasyncLightning.registriesSyncer.areRegistriesSynced())
            DatasyncLightning.registriesSyncer.sendRegistries(serverRequesting);
        else
            DatasyncLightning.LOGGER.error("Requested synced registries on this server but they are not synced !");
    }
}
