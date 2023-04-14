package fr.modcraftmc.datasync.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import fr.modcraftmc.datasync.DataSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class NetworkCommand extends CommandModule{

    @Override
    protected void buildCommand() {
        commandTree = Commands.literal("network")
                .then(Commands.literal("findPlayer")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> {
                                    findPlayer(context.getSource(), StringArgumentType.getString(context, "player"));
                                    return 1;
                                }
                        )))
                .then(Commands.literal("listPlayers")
                        .executes(context -> {
                            listPlayers(context.getSource());
                            return 1;
                        }
                ));
    }

    private void listPlayers(CommandSourceStack source) {
        var playersLocations = DataSync.playersLocation.getPlayersLocation();
        for (var entry : playersLocations.entrySet()){
            source.sendSuccess(Component.literal(String.format("player %s in server %s", entry.getKey(), entry.getValue().getName())), true);
        }
    }

    private void findPlayer(CommandSourceStack source, String player) {
        var playersLocations = DataSync.playersLocation.getPlayersLocation();
        for (var entry : playersLocations.entrySet()){
            if (entry.getKey().equals(player)){
                source.sendSuccess(Component.literal(String.format("player %s found in server %s", player, entry.getValue().getName())), true);
                return;
            }
        }
        source.sendSuccess(Component.literal(String.format("player %s not found", player)), true);
    }
}
