package fr.modcraftmc.datasync.tp.commands;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.arguments.NetworkPlayerArgument;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.tp.tpsync.TpRequest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TpCommand extends CommandModule {
    @Override
    protected void buildCommand() {
        COMMANDS.add(Commands.literal("tp")
                .then(Commands.argument("target", NetworkPlayerArgument.networkPlayer())
                        .then(Commands.argument("player", NetworkPlayerArgument.networkPlayer())
                                .executes(context -> tp(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "target"), NetworkPlayerArgument.getNetworkPlayer(context, "player"))))
                        .executes(context -> tp(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "target")))
                ));
    }

    private static ISyncPlayer getSyncPlayerFromCommand(CommandSourceStack source) {
        return CrossServerCoreAPI.getPlayer(source.getPlayer().getUUID()).orElseThrow();
    }

    private int tp(CommandSourceStack source, ISyncPlayer player) {
        if(!source.isPlayer()) {
            source.sendFailure(Component.literal("You must be a player to use this command"));
            return 0;
        }
        new TpRequest(getSyncPlayerFromCommand(source), player).fire();
        return 1;
    }

    private int tp(CommandSourceStack source, ISyncPlayer playerTarget, ISyncPlayer player) {
        new TpRequest(player, playerTarget).fire();
        return 1;
    }
}
