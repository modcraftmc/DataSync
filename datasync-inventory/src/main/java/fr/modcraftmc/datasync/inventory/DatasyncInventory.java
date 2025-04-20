package fr.modcraftmc.datasync.inventory;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod("datasyncinventory")
public class DatasyncInventory {
    public static final String MOD_ID = "datasyncinventory";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DatasyncInventory() {
        LOGGER.info("DatasyncInventory loading...");
        NeoForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);

        LOGGER.info("DatasyncInventory loaded !");
    }

    public void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event) {

    }
}
