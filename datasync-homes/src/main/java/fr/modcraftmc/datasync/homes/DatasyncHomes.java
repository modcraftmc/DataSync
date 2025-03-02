package fr.modcraftmc.datasync.homes;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.datasync.homes.commands.DatasyncHomesCommand;
import fr.modcraftmc.datasync.homes.messages.autoserializer.HomeSerializer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod("datasynchomes")
public class DatasyncHomes {
    public static final String MOD_ID = "datasynchomes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final HomeManager homeManager = new HomeManager();
    public DatasyncHomes() {
        LOGGER.info("DatasyncHomes loading...");

        NeoForge.EVENT_BUS.addListener(homeManager::onPlayerJoined);
        NeoForge.EVENT_BUS.addListener(this::commandResister);
        NeoForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);

        LOGGER.info("DatasyncHomes loaded !");
    }

    public void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event) {
        CrossServerCoreAPI.getMessageAutoPropertySerializer().registerFieldSerializer(new HomeSerializer());
    }


    public void commandResister(RegisterCommandsEvent event){
        new DatasyncHomesCommand(event.getDispatcher());
    }
}
