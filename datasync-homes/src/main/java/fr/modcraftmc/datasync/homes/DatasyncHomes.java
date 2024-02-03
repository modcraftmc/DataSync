package fr.modcraftmc.datasync.homes;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.datasync.homes.commands.DatasyncHomesCommand;
import fr.modcraftmc.datasync.homes.messages.ChangeGlobalHomesLimit;
import fr.modcraftmc.datasync.homes.messages.HomeTpRequest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("datasynchomes")
public class DatasyncHomes {
    public static final String MOD_ID = "datasynchomes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final HomeManager homeManager = new HomeManager();
    public DatasyncHomes() {
        LOGGER.info("DatasyncHomes loading...");

        MinecraftForge.EVENT_BUS.addListener(homeManager::onPlayerJoined);
        MinecraftForge.EVENT_BUS.addListener(this::commandResister);
        MinecraftForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);

        LOGGER.info("DatasyncHomes loaded !");
    }

    public void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event) {
        event.getInstance().registerCrossMessage(HomeTpRequest.MESSAGE_NAME, HomeTpRequest::deserialize);
        event.getInstance().registerCrossMessage(ChangeGlobalHomesLimit.MESSAGE_NAME, ChangeGlobalHomesLimit::deserialize);
        homeManager.register();
    }


    public void commandResister(RegisterCommandsEvent event){
        LOGGER.debug("Registering commands");
        new DatasyncHomesCommand(event.getDispatcher());
    }
}
