package fr.modcraftmc.datasync.tp.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import fr.modcraftmc.crossservercore.api.commands.NetworkPlayerSuggestionProvider;
import fr.modcraftmc.datasync.tp.tpsync.TpRequest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TpCommand extends CommandModule {
    @Override
    protected void buildCommand() {
        COMMANDS.add(Commands.literal("tp")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(new NetworkPlayerSuggestionProvider())
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(new NetworkPlayerSuggestionProvider())
                                .executes(context -> tp(context.getSource(), StringArgumentType.getString(context, "target"), StringArgumentType.getString(context, "player"))))
                        .executes(context -> tp(context.getSource(), StringArgumentType.getString(context, "target")))
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
