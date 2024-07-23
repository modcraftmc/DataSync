package fr.modcraftmc.datasync.tp.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
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
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(NetworkPlayerArgument::listSuggestions)
                        .executes(context -> tp(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "target")))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(NetworkPlayerArgument::listSuggestions)
                                .executes(context -> tp(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "target"), NetworkPlayerArgument.getNetworkPlayer(context, "player"))))
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
