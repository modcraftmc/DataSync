package fr.modcraftmc.datasync.ftbteams;

import com.mojang.logging.LogUtils;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.ftbteams.message.SyncTeamMessage;
import fr.modcraftmc.datasync.ftbteams.message.SyncTeams;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("datasyncftbteams")
public class DatasyncFtbTeam {
    public static final String MOD_ID = "datasyncftbteams";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final TeamsSynchronizer teamsSynchronizer = new TeamsSynchronizer();
    public DatasyncFtbTeam() {
        CrossServerCoreAPI.runWhenCSCIsReady(() -> {
            CrossServerCoreAPI.instance.registerCrossMessage(SyncTeams.MESSAGE_NAME, SyncTeams::deserialize);
            CrossServerCoreAPI.instance.registerCrossMessage(SyncTeamMessage.MESSAGE_NAME, SyncTeamMessage::deserialize);
        });
    }
}
