package fr.modcraftmc.datasync.tp.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.arguments.NetworkPlayerArgument;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
import fr.modcraftmc.datasync.tp.tpsync.TpaRequest;
import fr.modcraftmc.datasync.tp.tpsync.TpaRequestHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TpaCommand extends CommandModule{
    @Override
    protected void buildCommand() {
        ROOT_COMMANDS.add(Commands.literal("tpa")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(NetworkPlayerArgument::listSuggestions)
                        .executes(context -> tpa(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "target")))
                ));
        ROOT_COMMANDS.add(Commands.literal("tpaccept")
                .executes(context -> tpaccept(context.getSource()))
        );
        ROOT_COMMANDS.add(Commands.literal("tpdeny")
                .executes(context -> tpdeny(context.getSource()))
        );
    }

    private static ISyncPlayer getSyncPlayerFromCommand(CommandSourceStack source) {
        return CrossServerCoreAPI.getPlayer(source.getPlayer().getUUID()).orElseThrow();
    }

    private int tpa(CommandSourceStack source, ISyncPlayer target) {
        if(!source.isPlayer()) {
            source.sendFailure(Component.literal("You must be a player to use this command"));
            return 0;
        }

        new TpaRequest(getSyncPlayerFromCommand(source), target).fire();
        return 1;
    }

    private int tpaccept(CommandSourceStack source) {
        if(!source.isPlayer()) {
            source.sendFailure(Component.literal("You must be a player to use this command"));
            return 0;
        }
        TpaRequestHandler.accept(source.getPlayer());
        return 1;
    }

    private int tpdeny(CommandSourceStack source) {
        if(!source.isPlayer()) {
            source.sendFailure(Component.literal("You must be a player to use this command"));
            return 0;
        }
        TpaRequestHandler.deny(source.getPlayer());
        return 1;
    }
}