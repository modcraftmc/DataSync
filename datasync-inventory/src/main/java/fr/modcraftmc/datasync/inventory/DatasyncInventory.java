package fr.modcraftmc.datasync.inventory;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.CrossServerCoreAPI;
import fr.modcraftmc.datasync.inventory.message.TransferData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("datasyncinventory")
public class DatasyncInventory {
    public static final String MOD_ID = "datasyncinventory";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DatasyncInventory() {
        LOGGER.info("DatasyncInventory loading...");

        MinecraftForge.EVENT_BUS.addListener(PlayerDataSynchronizer::onPlayerJoined);
        MinecraftForge.EVENT_BUS.addListener(PlayerDataSynchronizer::onPlayerSave);
        MinecraftForge.EVENT_BUS.addListener(PlayerDataSynchronizer::onPlayerLeaved);

        CrossServerCoreAPI.runWhenCSCIsReady(() -> {
            PlayerDataSynchronizer.databasePlayerData = CrossServerCoreAPI.getOrCreateMongoCollection(References.PLAYER_DATA_COLLECTION_NAME);
            CrossServerCoreAPI.registerCrossMessage(TransferData.MESSAGE_NAME, TransferData::deserialize);
        });

        LOGGER.info("DatasyncInventory loaded !");
    }
}
