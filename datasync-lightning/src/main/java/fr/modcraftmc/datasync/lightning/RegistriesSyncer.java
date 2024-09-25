package fr.modcraftmc.datasync.lightning;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.crossservercore.api.events.SyncServerAttachEvent;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.datasync.lightning.message.RequestRegistriesSyncing;
import fr.modcraftmc.datasync.lightning.message.RegistriesSyncing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

public class RegistriesSyncer {
    private String syncServerSource;
    private boolean registriesSynced;
    private List<ISyncServer> existingServers = new ArrayList<>();
    private static final int DISCOVERY_TIME = 1000;

    public RegistriesSyncer(){
        registriesSynced = false;
        MinecraftForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onSyncServerAttached);
    }

    public boolean areRegistriesSynced(){
        return registriesSynced;
    }

    public void setRegistriesSynced(String syncServerSource){
        DatasyncLightning.LOGGER.info("Registries synced with server " + syncServerSource);
        this.syncServerSource = syncServerSource;
        registriesSynced = true;
    }

    private void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event){
        Thread syncThread = new Thread(() -> {
            try {
                Thread.sleep(DISCOVERY_TIME);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            ServerLifecycleHooks.getCurrentServer().execute(this::onServerDiscoveryEnded);
        });
        syncThread.start();
    }

    private void onSyncServerAttached(SyncServerAttachEvent event){
        if(event.getAttachType() == SyncServerAttachEvent.AttachType.EXISTING){
            existingServers.add(event.getSyncServer());
        }
    }

    private void onServerDiscoveryEnded(){
        if(existingServers.isEmpty()){ // Assuming no existing server, defining this one as synced
            setRegistriesSynced(CrossServerCoreAPI.getServerName());
            return;
        }

        requestRegistriesSyncing(existingServers.get(0));
    }

    private void requestRegistriesSyncing(ISyncServer syncServer){
        DatasyncLightning.LOGGER.info("Requesting registries syncing to server " + syncServer.getName());
        syncServer.sendMessage(new RequestRegistriesSyncing());
    }

    public void sendRegistries(ISyncServer syncServer){
        syncServer.sendMessage(new RegistriesSyncing(RegistryManager.ACTIVE.takeSnapshot(false)));
    }
}
