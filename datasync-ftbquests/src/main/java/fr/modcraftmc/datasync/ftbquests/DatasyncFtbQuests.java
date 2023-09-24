package fr.modcraftmc.datasync.ftbquests;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.ftbquests.commands.DatasyncFtbQuestsCommand;
import fr.modcraftmc.datasync.ftbquests.message.SyncQuests;
import fr.modcraftmc.datasync.ftbquests.message.SyncTeamQuests;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("datasyncftbquests")
public class DatasyncFtbQuests {
    public static final String MOD_ID = "datasyncftbquests";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final QuestsSynchronizer questsSynchronizer = new QuestsSynchronizer();

    public DatasyncFtbQuests() {
        MinecraftForge.EVENT_BUS.addListener(this::commandResister);

        CrossServerCoreAPI.runWhenCSCIsReady(() -> {
            CrossServerCoreAPI.instance.registerCrossMessage(SyncQuests.MESSAGE_NAME, SyncQuests::deserialize);
            CrossServerCoreAPI.instance.registerCrossMessage(SyncTeamQuests.MESSAGE_NAME, SyncTeamQuests::deserialize);
        });
    }

    public void commandResister(RegisterCommandsEvent event){
        DatasyncFtbQuests.LOGGER.debug("Registering commands");
        new DatasyncFtbQuestsCommand(event.getDispatcher());
    }
}
