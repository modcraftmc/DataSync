package fr.modcraftmc.datasync.tp;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.datasync.tp.commands.DatasyncTpCommand;
import fr.modcraftmc.datasync.tp.message.TpRequestMessage;
import fr.modcraftmc.datasync.tp.message.TpaRequestMessage;
import fr.modcraftmc.datasync.tp.tpsync.TpRequestHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("datasynctp")
public class DatasyncTp {
    public static final String MOD_ID = "datasynctp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DatasyncTp() {
        LOGGER.info("DatasyncTp loading...");

        MinecraftForge.EVENT_BUS.addListener(this::commandResister);
        MinecraftForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);
        MinecraftForge.EVENT_BUS.addListener(TpRequestHandler::onPlayerJoined);

        LOGGER.info("DatasyncTp loaded !");
    }

    public void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event) {
        event.getInstance().registerCrossMessage(TpaRequestMessage.MESSAGE_NAME, TpaRequestMessage::deserialize);
        event.getInstance().registerCrossMessage(TpRequestMessage.MESSAGE_NAME, TpRequestMessage::deserialize);
    }

    public void commandResister(RegisterCommandsEvent event){
        DatasyncTp.LOGGER.debug("Registering commands");
        new DatasyncTpCommand(event.getDispatcher());
    }
}
