package fr.modcraftmc.datasync.waystones;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.CrossServerCoreAPI;
import fr.modcraftmc.datasync.waystones.message.TeleportToWaystone;
import fr.modcraftmc.datasync.waystones.message.UpdateWaystoneMessage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(DatasyncWaystones.MOD_ID)
public class DatasyncWaystones {

    public static final String MOD_ID = "datasyncwaystones";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DatasyncWaystones() {
        LOGGER.info("DatasyncWaystones loading...");

        MinecraftForge.EVENT_BUS.addListener(WaystoneTpHandler::onPlayerJoined);
        CrossServerCoreAPI.runWhenCSCIsReady(() -> {
            CrossServerCoreAPI.registerCrossMessage(UpdateWaystoneMessage.MESSAGE_NAME, UpdateWaystoneMessage::deserialize);
            CrossServerCoreAPI.registerCrossMessage(TeleportToWaystone.MESSAGE_NAME, TeleportToWaystone::deserialize);
        });
        LOGGER.info("DatasyncWaystones loaded !");
    }
}