package fr.modcraftmc.datasync.homes.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.arguments.NetworkPlayerArgument;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncPlayer;
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
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(DatasyncHomes.homeManager.getHomeNames(getSyncPlayerFromCommand(context.getSource())), builder))
                        .executes(context -> selfHomeTeleport(context.getSource(), StringArgumentType.getString(context, "targethome")))));


        ROOT_COMMANDS.add(Commands.literal("homes")
                .executes(context -> showPlayerHomes(context.getSource()))
                .then(Commands.argument("player", NetworkPlayerArgument.networkPlayer())
                        .executes(context -> showPlayerHomes(context.getSource(), NetworkPlayerArgument.getNetworkPlayer(context, "player"), true))
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
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(DatasyncHomes.homeManager.getHomeNames(getSyncPlayerFromCommand(context.getSource())), builder))
                        .executes(context -> deleteHome(context.getSource(), StringArgumentType.getString(context, "name")))));


    }

    private static ISyncPlayer getSyncPlayerFromCommand(CommandSourceStack source) {
        return CrossServerCoreAPI.getPlayer(source.getPlayer().getUUID()).orElseThrow();
    }

    private int createHome(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ISyncPlayer syncPlayer = getSyncPlayerFromCommand(source);

        if(DatasyncHomes.homeManager.homeExists(syncPlayer, name)){
            source.sendFailure(Component.literal("Home " + name + " already exists !").withStyle(style -> style.withColor(ChatFormatting.RED)));
            return 0;
        }

        if(!DatasyncHomes.homeManager.canCreateHome(syncPlayer)){
            source.sendFailure(Component.literal("You can't create more homes !").withStyle(style -> style.withColor(ChatFormatting.RED)));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        ServerLevel dimension = player.getLevel();
        DatasyncHomes.homeManager.createHome(syncPlayer, name, pos.getX(), pos.getY(), pos.getZ(), SerializationUtil.ToJsonElement(dimension.dimension(), Registry.DIMENSION_REGISTRY).toString());
        source.sendSuccess(Component.literal("Home " + name + " created !").withStyle(style -> style.withColor(ChatFormatting.GOLD)), false);
        return 1;
    }

    private int deleteHome(CommandSourceStack source, String homeName) throws CommandSyntaxException {
        ISyncPlayer player = getSyncPlayerFromCommand(source);
        if(!DatasyncHomes.homeManager.homeExists(player, homeName)){
            source.sendFailure(Component.literal("Home " + homeName + " doesn't exist !").withStyle(style -> style.withColor(ChatFormatting.RED)));
            return 0;
        }
        DatasyncHomes.homeManager.deleteHome(player, homeName);
        source.sendSuccess(Component.literal("Home " + homeName + " deleted !").withStyle(style -> style.withColor(ChatFormatting.GOLD)), false);
        return 1;
    }

    private int selfHomeTeleport(CommandSourceStack source, String target) throws CommandSyntaxException {
        ISyncPlayer player = getSyncPlayerFromCommand(source);
        if(!DatasyncHomes.homeManager.homeExists(player, target)){
            source.sendFailure(Component.literal("Home " + target + " doesn't exist !").withStyle(style -> style.withColor(ChatFormatting.RED)));
            return 0;
        }
        DatasyncHomes.homeManager.tryTeleportPlayerToHome(player, player, target);
        return 1;
    }

    private int homeTeleport(CommandSourceStack source, ISyncPlayer playerTarget, String homeTarget) throws CommandSyntaxException {
        ISyncPlayer player = getSyncPlayerFromCommand(source);
        DatasyncHomes.homeManager.tryTeleportPlayerToHome(player, playerTarget, homeTarget);
        return 1;
    }

    private int showPlayerHomes(CommandSourceStack source, ISyncPlayer player, boolean selfPlayerHomes) throws CommandSyntaxException {
        List<String> homeNames = DatasyncHomes.homeManager.getHomeNames(player);
        MutableComponent message = Component.literal(player.getName() + "'s homes :").withStyle(style -> style.withColor(ChatFormatting.GOLD));
        for (String homeName : homeNames) {
            message.append("\n");

            if(selfPlayerHomes)
                message.append(Component.literal(String.format("[%s] %s", homeName, ChatFormatting.GRAY)).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + homeName))).withStyle(style -> style.withColor(ChatFormatting.GREEN)));
            else
                message.append(Component.literal(String.format("[%s] M%s", homeName, ChatFormatting.GRAY)).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + player + " " + homeName))).withStyle(style -> style.withColor(ChatFormatting.GREEN)));

            if (DatasyncHomes.homeManager.isLocalHome(player,  homeName)) {
                message.append("(local)");
            }
        }

        source.sendSuccess(message, false);
        return 1;
    }

    private int showPlayerHomes(CommandSourceStack source) throws CommandSyntaxException {
        return showPlayerHomes(source, getSyncPlayerFromCommand(source), true);
    }

}