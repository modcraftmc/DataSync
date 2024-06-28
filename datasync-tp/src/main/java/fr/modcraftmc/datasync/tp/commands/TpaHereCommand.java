package fr.modcraftmc.datasync.tp.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import fr.modcraftmc.crossservercore.api.commands.NetworkPlayerSuggestionProvider;
import fr.modcraftmc.datasync.tp.tpsync.TpaHereRequest;
import fr.modcraftmc.datasync.tp.tpsync.TpaHereRequestHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TpaHereCommand extends CommandModule{
    @Override
    protected void buildCommand() {
        ROOT_COMMANDS.add(Commands.literal("tpahere")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(new NetworkPlayerSuggestionProvider())
                        .executes(context -> tpa(context.getSource(), StringArgumentType.getString(context, "target")))
                ));
        ROOT_COMMANDS.add(Commands.literal("tpahereaccept")
                .executes(context -> tpaccept(context.getSource()))
        );
        ROOT_COMMANDS.add(Commands.literal("tpaheredeny")
                .executes(context -> tpdeny(context.getSource()))
        );
    }

    private int tpa(CommandSourceStack source, String target) {
        if(!source.isPlayer()) {
            source.sendFailure(Component.literal("You must be a player to use this command"));
            return 0;
        }
        new TpaHereRequest(source.getPlayer().getName().getString(), target).fire();
        return 1;
    }

    private int tpaccept(CommandSourceStack source) {
        if(!source.isPlayer()) {
            source.sendFailure(Component.literal("You must be a player to use this command"));
            return 0;
        }
        TpaHereRequestHandler.accept(source.getPlayer());
        return 1;
    }

    private int tpdeny(CommandSourceStack source) {
        if(!source.isPlayer()) {
            source.sendFailure(Component.literal("You must be a player to use this command"));
            return 0;
        }
        TpaHereRequestHandler.deny(source.getPlayer());
        return 1;
    }
}