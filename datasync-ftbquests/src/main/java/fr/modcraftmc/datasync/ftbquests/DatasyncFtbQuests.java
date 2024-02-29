package fr.modcraftmc.datasync.ftbquests;

import com.mojang.logging.LogUtils;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.events.CrossServerCoreReadyEvent;
import fr.modcraftmc.datasync.ftbquests.commands.DatasyncFtbQuestsCommand;
import fr.modcraftmc.datasync.ftbquests.message.SyncQuests;
import fr.modcraftmc.datasync.ftbquests.message.SyncTeamQuests;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("datasyncftbquests")
public class DatasyncFtbQuests {
    public static final String MOD_ID = "datasyncftbquests";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final QuestsSynchronizer questsSynchronizer = new QuestsSynchronizer();

    public DatasyncFtbQuests() {
        MinecraftForge.EVENT_BUS.addListener(this::commandResister);
        MinecraftForge.EVENT_BUS.addListener(this::onCrossServerCoreReadyEvent);
    }

    public void onCrossServerCoreReadyEvent(CrossServerCoreReadyEvent event) {
        if(!ModList.get().isLoaded(References.FTBQUESTS_MOD_ID)) return;
        event.getInstance().registerCrossMessage(SyncQuests.MESSAGE_NAME, SyncQuests::deserialize);
        event.getInstance().registerCrossMessage(SyncTeamQuests.MESSAGE_NAME, SyncTeamQuests::deserialize);
        questsSynchronizer.register();
        questsSynchronizer.loadTeamsQuests();
    }

    public void commandResister(RegisterCommandsEvent event){
        DatasyncFtbQuests.LOGGER.debug("Registering commands");
        new DatasyncFtbQuestsCommand(event.getDispatcher());
    }
}
