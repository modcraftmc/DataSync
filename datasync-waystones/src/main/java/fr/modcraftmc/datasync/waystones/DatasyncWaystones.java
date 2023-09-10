package fr.modcraftmc.datasync.waystones;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercoreapi.CrossServerCoreAPI;
import fr.modcraftmc.datasync.waystones.message.TeleportToWaystone;
import fr.modcraftmc.datasync.waystones.message.UpdateWaystone;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(DatasyncWaystones.MOD_ID)
public class DatasyncWaystones {

    public static final String MOD_ID = "datasyncwaystones";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final WaystoneManager waystoneManager = new WaystoneManager();

    public DatasyncWaystones() {
        LOGGER.info("DatasyncWaystones loading...");

        MinecraftForge.EVENT_BUS.addListener(waystoneManager::onPlayerJoined);
        CrossServerCoreAPI.runWhenCSCIsReady(() -> {
            CrossServerCoreAPI.instance.registerCrossMessage(UpdateWaystone.MESSAGE_NAME, UpdateWaystone::deserialize);
            CrossServerCoreAPI.instance.registerCrossMessage(TeleportToWaystone.MESSAGE_NAME, TeleportToWaystone::deserialize);
        });
        LOGGER.info("DatasyncWaystones loaded !");
    }
}