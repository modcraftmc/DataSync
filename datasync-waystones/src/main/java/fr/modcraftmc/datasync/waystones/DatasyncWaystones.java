package fr.modcraftmc.datasync.waystones;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.datasync.waystones.message.autoserializer.WaystoneSerialier;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(DatasyncWaystones.MOD_ID)
public class DatasyncWaystones {

    public static final String MOD_ID = "datasyncwaystones";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final WaystoneManager waystoneManager = new WaystoneManager();

    public DatasyncWaystones() {
        LOGGER.info("DatasyncWaystones loading...");

        NeoForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);

        LOGGER.info("DatasyncWaystones loaded !");
    }

    private void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event) {
        CrossServerCoreAPI.getMessageAutoPropertySerializer().registerFieldSerializer(new WaystoneSerialier());
    }


}