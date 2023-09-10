package fr.modcraftmc.datasync.homes.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.modcraftmc.crossservercoreapi.commands.arguments.NetworkPlayerArgument;
import fr.modcraftmc.datasync.homes.DatasyncHomes;
import fr.modcraftmc.datasync.homes.serialization.SerializationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class HomeCommand extends CommandModule {

    @Override
    protected void buildCommand() {
        ROOT_COMMANDS.add(Commands.literal("home")
                .executes(context -> showPlayerHomes(context.getSource()))
                .then(Commands.argument("targethome", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(DatasyncHomes.homeManager.getHomeNames(context.getSource().getPlayerOrException().getName().getString()), builder))
                        .executes(context -> selfHomeTeleport(context.getSource(), StringArgumentType.getString(context, "targethome")))));


        ROOT_COMMANDS.add(Commands.literal("homes")
                .executes(context -> showPlayerHomes(context.getSource()))
                .then(Commands.argument("player", NetworkPlayerArgument.networkPlayer())
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("playerhome", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(DatasyncHomes.homeManager.getHomeNames(NetworkPlayerArgument.getNetworkPlayer(context, "player")), builder))
                                .executes(context -> homeTeleport(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "player"), StringArgumentType.getString(context, "playerhome"))))));


        ROOT_COMMANDS.add(Commands.literal("listhomes")
                .then(Commands.argument("player", NetworkPlayerArgument.networkPlayer())
                        .executes(context -> showPlayerHomes(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "player"), false))));

        ROOT_COMMANDS.add(Commands.literal("sethome")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(context -> createHome(context.getSource(), StringArgumentType.getString(context, "name")))));

        ROOT_COMMANDS.add(Commands.literal("delhome")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(DatasyncHomes.homeManager.getHomeNames(context.getSource().getPlayerOrException().getName().getString()), builder))
                        .executes(context -> deleteHome(context.getSource(), StringArgumentType.getString(context, "name")))));


    }

    private int createHome(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String playerName = player.getName().getString();

        if(DatasyncHomes.homeManager.homeExists(playerName, name)){
            source.sendFailure(Component.literal("Home " + name + " already exists !").withStyle(style -> style.withColor(ChatFormatting.RED)));
            return 0;
        }

        if(!DatasyncHomes.homeManager.canCreateHome(playerName)){
            source.sendFailure(Component.literal("You can't create more homes !").withStyle(style -> style.withColor(ChatFormatting.RED)));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        ServerLevel dimension = player.getLevel();
        DatasyncHomes.homeManager.createHome(playerName, name, pos.getX(), pos.getY(), pos.getZ(), SerializationUtil.ToJsonElement(dimension.dimension(), Registry.DIMENSION_REGISTRY).toString());
        source.sendSuccess(Component.literal("Home " + name + " created !").withStyle(style -> style.withColor(ChatFormatting.GOLD)), false);
        return 1;
    }

    private int deleteHome(CommandSourceStack source, String homeName) throws CommandSyntaxException {
        String playerName = source.getPlayerOrException().getName().getString();
        if(!DatasyncHomes.homeManager.homeExists(playerName, homeName)){
            source.sendFailure(Component.literal("Home " + homeName + " doesn't exist !").withStyle(style -> style.withColor(ChatFormatting.RED)));
            return 0;
        }
        DatasyncHomes.homeManager.deleteHome(source.getPlayerOrException().getName().getString(), homeName);
        source.sendSuccess(Component.literal("Home " + homeName + " deleted !").withStyle(style -> style.withColor(ChatFormatting.GOLD)), false);
        return 1;
    }

    private int selfHomeTeleport(CommandSourceStack source, String target) throws CommandSyntaxException {
        String playerName = source.getPlayerOrException().getName().getString();
        if(!DatasyncHomes.homeManager.homeExists(playerName, target)){
            source.sendFailure(Component.literal("Home " + target + " doesn't exist !").withStyle(style -> style.withColor(ChatFormatting.RED)));
            return 0;
        }
        DatasyncHomes.homeManager.tryTeleportPlayerToHome(playerName, playerName, target);
        return 1;
    }

    private int homeTeleport(CommandSourceStack source, String playerName, String target) throws CommandSyntaxException {
        DatasyncHomes.homeManager.tryTeleportPlayerToHome(source.getPlayerOrException().getName().getString(), playerName, target);
        return 1;
    }

    private int showPlayerHomes(CommandSourceStack source, String playerName, boolean selfPlayerHomes) throws CommandSyntaxException {
        List<String> homeNames = DatasyncHomes.homeManager.getHomeNames(playerName);
        MutableComponent message = Component.literal(playerName + "'s homes :").withStyle(style -> style.withColor(ChatFormatting.GOLD));
        for (String homeName : homeNames) {
            message.append("\n");
            if(selfPlayerHomes)
                message.append(Component.literal(String.format("[%s]", homeName)).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + homeName))).withStyle(style -> style.withColor(ChatFormatting.GREEN)));
            else
                message.append(Component.literal(String.format("[%s]", homeName)).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + playerName + " " + homeName))).withStyle(style -> style.withColor(ChatFormatting.GREEN)));
        }

        source.sendSuccess(message, false);
        return 1;
    }

    private int showPlayerHomes(CommandSourceStack source) throws CommandSyntaxException {
        return showPlayerHomes(source, source.getPlayerOrException().getName().getString(), true);
    }

}