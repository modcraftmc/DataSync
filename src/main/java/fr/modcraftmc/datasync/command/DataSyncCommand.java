package fr.modcraftmc.datasync.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.modcraftmc.datasync.DataSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class DataSyncCommand {
    public static final String COMMAND_NAME = "datasync";

    public DataSyncCommand(CommandDispatcher<CommandSourceStack> dispatcher){
        new RabbitmqTestCommand();
        new SerializerTestCommand();
        new NetworkCommand();
        register(dispatcher);
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> commandTree = Commands.literal(COMMAND_NAME);
        CommandModule.COMMANDS.forEach(commandTree::then);
        commandTree.then(Commands.literal("reload").executes(context -> {
            DataSync.loadConfig();
            context.getSource().sendSuccess(Component.literal("reloaded !"), true);
            return 1;
        }));
        dispatcher.register(commandTree);
    }
}
