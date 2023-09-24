package fr.modcraftmc.datasync.ftbquests.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DatasyncFtbQuestsCommand {
    public static final String COMMAND_NAME = "datasync";

    public DatasyncFtbQuestsCommand(CommandDispatcher<CommandSourceStack> dispatcher){
        new SyncCommand();
        register(dispatcher);
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> commandTree = Commands.literal(COMMAND_NAME);
        CommandModule.COMMANDS.forEach(commandTree::then);
        dispatcher.register(commandTree);
        CommandModule.ROOT_COMMANDS.forEach(dispatcher::register);
    }
}
