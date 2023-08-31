package fr.modcraftmc.datasync.tp.commands;

import fr.modcraftmc.crossservercore.command.arguments.NetworkPlayerArgument;
import fr.modcraftmc.datasync.tp.tpsync.TpRequest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TpCommand extends CommandModule{
    @Override
    protected void buildCommand() {
        COMMANDS.add(Commands.literal("tp")
                .then(Commands.argument("target", NetworkPlayerArgument.networkPlayer())
                        .then(Commands.argument("player", NetworkPlayerArgument.networkPlayer())
                                .executes(context -> tp(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "target"), NetworkPlayerArgument.getNetworkPlayer(context, "player")))
                        )
                        .executes(context -> tp(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "target")))
                ));
    }

    private int tp(CommandSourceStack source, String player) {
        if(!source.isPlayer()) {
            source.sendFailure(Component.literal("You must be a player to use this command"));
            return 0;
        }
        new TpRequest(source.getPlayer().getName().getString(), player).fire();
        return 1;
    }

    private int tp(CommandSourceStack source, String playerTarget, String player) {
        new TpRequest(player, playerTarget).fire();
        return 1;
    }
}
