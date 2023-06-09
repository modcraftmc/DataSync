package fr.modcraftmc.datasync.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.command.arguments.NetworkPlayerArgument;
import fr.modcraftmc.datasync.ftbsync.FTBSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class NetworkCommand extends CommandModule{

    @Override
    protected void buildCommand() {
        commandTree = Commands.literal("network")
                .then(Commands.literal("findPlayer")
                        .then(Commands.argument("player", NetworkPlayerArgument.networkPlayer())
                                .executes(context -> {
                                    findPlayer(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "player"));
                                    return 1;
                                }
                        )))
                .then(Commands.literal("listPlayers")
                        .executes(context -> {
                            listPlayers(context.getSource());
                            return 1;
                        }
                ))
                .then(Commands.literal("isTeamOnline")
                        .then(Commands.argument("player", NetworkPlayerArgument.networkPlayer())
                                .executes(context -> {
                                    isTeamOnline(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "player"));
                                    return 1;
                                })
                        )
                );
    }

    private void isTeamOnline(CommandSourceStack source, String playerName) {
        if(FTBSync.FTBTeamsLoaded){
            final boolean[] found = {false};
            DataSync.LOGGER.debug("Searching for team of player " + playerName + "...");
            FTBTeamsAPI.getManager().getKnownPlayers().forEach((uuid, playerTeam) -> {
                DataSync.LOGGER.debug("Comparing team player name " + playerTeam.playerName + " with player " + playerName);
                if(playerTeam.playerName.equals(playerName)){
                    found[0] = true;
                    if(playerTeam.online)
                        source.sendSuccess(Component.literal(String.format("team of %s is online", playerName)), true);
                    else
                        source.sendSuccess(Component.literal(String.format("team of %s is offline", playerName)), true);
                }
            });
            if(!found[0])
                source.sendFailure(Component.literal(String.format("team of %s not found", playerName)));
        }
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
