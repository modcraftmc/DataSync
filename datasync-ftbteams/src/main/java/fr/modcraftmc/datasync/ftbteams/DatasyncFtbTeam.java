package fr.modcraftmc.datasync.ftbteams;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("datasyncftbteams")
public class DatasyncFtbTeam {
    public static final String MOD_ID = "datasyncftbteams";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final TeamsSynchronizer teamsSynchronizer = new TeamsSynchronizer();
    public DatasyncFtbTeam() {

    }
}
