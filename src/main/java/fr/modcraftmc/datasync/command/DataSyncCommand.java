package fr.modcraftmc.datasync.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.modcraftmc.datasync.DataSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class DataSyncCommand {
    public static final String COMMAND_NAME = "datasync";
    public static final ArrayList<LiteralArgumentBuilder<CommandSourceStack>> COMMANDS = new ArrayList<>();

    public DataSyncCommand(CommandDispatcher<CommandSourceStack> dispatcher){
        new SerializerTestCommand(this);
        new RabbitmqTestCommand(this);
        register(dispatcher);
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> commandTree = Commands.literal(COMMAND_NAME);
        COMMANDS.forEach(commandTree::then);
        commandTree.then(Commands.literal("reload").executes(context -> {
            DataSync.loadConfig();
            context.getSource().sendSuccess(Component.literal("reloaded !"), true);
            return 1;
        }));
        dispatcher.register(commandTree);
    }

    public void registerCommand(LiteralArgumentBuilder<CommandSourceStack> commandTree){
        if(commandTree != null)
            COMMANDS.add(commandTree);
    }
}
