package fr.modcraftmc.datasync.lightning.message;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.annotation.AutoRegister;
import fr.modcraftmc.crossservercore.api.annotation.AutoSerialize;
import fr.modcraftmc.crossservercore.api.message.BaseMessage;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.datasync.lightning.DatasyncLightning;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;

import java.util.HashMap;
import java.util.Map;

@AutoRegister("registries_syncing")
public class RegistriesSyncing extends BaseMessage {
    @AutoSerialize
    private Map<ResourceLocation, ForgeRegistry.Snapshot> registriesToSync = new HashMap<>();
    @AutoSerialize
    private ISyncServer source;

    private RegistriesSyncing() {
    }

    public RegistriesSyncing(Map<ResourceLocation, ForgeRegistry.Snapshot> registriesToSync) {
        this.registriesToSync = registriesToSync;
        this.source = CrossServerCoreAPI.getServer();
    }

    @Override
    public void handle() {
        DatasyncLightning.LOGGER.info("Syncing registries...");
        GameData.injectSnapshot(registriesToSync, false, false);
        DatasyncLightning.registriesSyncer.setRegistriesSynced(source.getName());
    }

    public Map<ResourceLocation, ForgeRegistry.Snapshot> getRegistriesToSync() {
        return registriesToSync;
    }
}
