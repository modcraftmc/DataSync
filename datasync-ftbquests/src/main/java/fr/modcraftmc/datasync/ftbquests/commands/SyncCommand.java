package fr.modcraftmc.datasync.ftbquests.commands;

import fr.modcraftmc.datasync.ftbquests.DatasyncFtbQuests;
import net.minecraft.commands.Commands;

public class SyncCommand extends CommandModule{
    @Override
    protected void buildCommand() {
        COMMANDS.add(Commands.literal("syncquests")
                .executes(context -> syncQuests()));
    }

    private int syncQuests() {
        DatasyncFtbQuests.questsSynchronizer.syncQuests();
        return 1;
    }
}
