package fr.modcraftmc.datasync.ftbquests.commands;

import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import fr.modcraftmc.datasync.ftbquests.DatasyncFtbQuests;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class SyncCommand extends CommandModule{
    @Override
    protected void buildCommand() {
        COMMANDS.add(Commands.literal("syncquests")
                .executes(context -> syncQuests()));
        COMMANDS.add(Commands.literal("syncteamquests")
                .executes(context -> syncTeamQuest(context.getSource())));

    }

    private int syncQuests() {
        DatasyncFtbQuests.questsSynchronizer.syncQuests();
        return 1;
    }

    private int syncTeamQuest(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        Team team = FTBTeamsAPI.getPlayerTeam(player);
        DatasyncFtbQuests.questsSynchronizer.syncTeamQuests(FTBQuests.PROXY.getQuestFile(false).getData(team));
        return 1;
    }
}
