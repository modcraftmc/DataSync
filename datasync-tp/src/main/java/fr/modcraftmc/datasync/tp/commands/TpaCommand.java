package fr.modcraftmc.datasync.tp.commands;

import fr.modcraftmc.crossservercore.command.arguments.NetworkPlayerArgument;
import fr.modcraftmc.datasync.tp.tpsync.TpaRequest;
import fr.modcraftmc.datasync.tp.tpsync.TpaRequestHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TpaCommand extends CommandModule{
    @Override
    protected void buildCommand() {
        ROOT_COMMANDS.add(Commands.literal("tpa")
                .then(Commands.argument("target", NetworkPlayerArgument.networkPlayer())
                        .executes(context -> {
                            tpa(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "target"));
                            return 1;
                        })
                ));
        ROOT_COMMANDS.add(Commands.literal("tpaccept")
                .executes(context -> {
                    tpaccept(context.getSource());
                    return 1;
                })
        );
        ROOT_COMMANDS.add(Commands.literal("tpdeny")
                .executes(context -> {
                    tpdeny(context.getSource());
                    return 1;
                })
        );
    }

    private void tpa(CommandSourceStack source, String target) {
        if(!source.isPlayer())
            source.sendFailure(Component.literal("You must be a player to use this command"));
        new TpaRequest(source.getPlayer().getName().getString(), target).fire();
    }

    private void tpaccept(CommandSourceStack source) {
        if(!source.isPlayer())
            source.sendFailure(Component.literal("You must be a player to use this command"));
        TpaRequestHandler.accept(source.getPlayer());
    }

    private void tpdeny(CommandSourceStack source) {
        if(!source.isPlayer())
            source.sendFailure(Component.literal("You must be a player to use this command"));
        TpaRequestHandler.deny(source.getPlayer());
    }
}