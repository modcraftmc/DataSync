package fr.modcraftmc.datasync.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.modcraftmc.datasync.rabbitmq.RabbitmqTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class RabbitmqTestCommand {
    private LiteralArgumentBuilder<CommandSourceStack> commandTree;

    public RabbitmqTestCommand(DataSyncCommand dataSyncCommand) {
        buildCommand();
        dataSyncCommand.registerCommand(commandTree);
    }

    private void buildCommand() {
        commandTree = Commands.literal("rabbitmq")
                .then(Commands.literal("send")
                        .executes(context -> {
                            send(context.getSource());
                            return 1;
                        }))
                .then(Commands.literal("prepareReceive")
                        .executes(context -> {
                            receive(context.getSource());
                            return 1;
                        }));
    }

    private void send(CommandSourceStack source) {
        RabbitmqTest.sendMessage();
        source.sendSuccess(Component.literal("success"), true);
    }

    private void receive(CommandSourceStack source) {
        RabbitmqTest.prepareReceiveMessage();
        source.sendSuccess(Component.literal("success"), true);
    }
}
