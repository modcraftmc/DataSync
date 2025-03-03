package fr.modcraftmc.datasync.lightning;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.datasync.lightning.message.autoserializer.RegistrySnapshotSerializer;
import fr.modcraftmc.datasync.lightning.message.autoserializer.ResourceLocationSerializer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(DatasyncLightning.MOD_ID)
public class DatasyncLightning {
    public static final String MOD_ID = "datasynclightning";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static RegistriesSyncer registriesSyncer = new RegistriesSyncer();

    public DatasyncLightning() {
        LOGGER.info("DatasyncLightning loading...");

        NeoForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);

        LOGGER.info("DatasyncLightning loaded !");
    }

    private void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event) {
        CrossServerCoreAPI.getMessageAutoPropertySerializer().registerFieldSerializer(new RegistrySnapshotSerializer());
        CrossServerCoreAPI.getMessageAutoPropertySerializer().registerFieldSerializer(new ResourceLocationSerializer());
    }

    private void onServerAttachEvent(){

    }
}
