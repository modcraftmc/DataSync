package fr.modcraftmc.datasync.ftbquests;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.datasync.ftbquests.commands.DatasyncFtbQuestsCommand;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod("datasyncftbquests")
public class DatasyncFtbQuests {
    public static final String MOD_ID = "datasyncftbquests";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final QuestsSynchronizer questsSynchronizer = new QuestsSynchronizer();

    public DatasyncFtbQuests() {
        NeoForge.EVENT_BUS.addListener(this::commandResister);
    }


    public void commandResister(RegisterCommandsEvent event){
        DatasyncFtbQuests.LOGGER.debug("Registering commands");
        new DatasyncFtbQuestsCommand(event.getDispatcher());
    }
}
