package fr.modcraftmc.datasync.homes.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DatasyncHomesCommand {
    public static final String COMMAND_NAME = "datasync";

    public DatasyncHomesCommand(CommandDispatcher<CommandSourceStack> dispatcher){
        new HomeCommand();
        new HomesLimitCommand();
        register(dispatcher);
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> commandTree = Commands.literal(COMMAND_NAME);
        CommandModule.COMMANDS.forEach(commandTree::then);
        dispatcher.register(commandTree);
        CommandModule.ROOT_COMMANDS.forEach(dispatcher::register);
    }
}
