package fr.modcraftmc.datasync.tp;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.datasync.tp.commands.DatasyncTpCommand;
import fr.modcraftmc.datasync.tp.tpsync.TpRequestHandler;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod("datasynctp")
public class DatasyncTp {
    public static final String MOD_ID = "datasynctp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DatasyncTp() {
        LOGGER.info("DatasyncTp loading...");

        NeoForge.EVENT_BUS.addListener(this::commandResister);
        NeoForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);
        NeoForge.EVENT_BUS.addListener(TpRequestHandler::onPlayerJoined);

        LOGGER.info("DatasyncTp loaded !");
    }

    public void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event) {

    }

    public void commandResister(RegisterCommandsEvent event){
        new DatasyncTpCommand(event.getDispatcher());
    }
}
