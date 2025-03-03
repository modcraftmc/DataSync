package fr.modcraftmc.datasync.lightning.message;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.datasync.lightning.DatasyncLightning;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.RegistrySnapshot;

import java.util.HashMap;
import java.util.Map;

@AutoRegister("registries_syncing")
public class RegistriesSyncing extends BaseMessage {
    @AutoSerialize
    private Map<ResourceLocation, RegistrySnapshot> registriesToSync = new HashMap<>();
    @AutoSerialize
    private ISyncServer source;

    private RegistriesSyncing() {
    }

    public RegistriesSyncing(Map<ResourceLocation, RegistrySnapshot> registriesToSync) {
        this.registriesToSync = registriesToSync;
        this.source = CrossServerCoreAPI.getServer();
    }

    @Override
    public void handle() {
        DatasyncLightning.LOGGER.info("Syncing registries...");
        RegistryManager.applySnapshot(registriesToSync, false, false);
        DatasyncLightning.registriesSyncer.setRegistriesSynced(source.getName());
    }

    public Map<ResourceLocation, RegistrySnapshot> getRegistriesToSync() {
        return registriesToSync;
    }
}
