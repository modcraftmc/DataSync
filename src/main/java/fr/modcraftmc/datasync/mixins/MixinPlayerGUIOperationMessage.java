package fr.modcraftmc.datasync.mixins;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.TeamRank;
import dev.ftb.mods.ftbteams.net.PlayerGUIOperationMessage;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.message.SendMessageMessage;
import fr.modcraftmc.datasync.networkidentity.SyncServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = PlayerGUIOperationMessage.class, remap = false)
public class MixinPlayerGUIOperationMessage
{
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;m_6846_()Lnet/minecraft/server/players/PlayerList;", ordinal = 1) , method = "processTarget")
    protected void onProcessInvite(ServerPlayer sourcePlayer, TeamRank senderRank, PartyTeam partyTeam, UUID targetId, CallbackInfo ci){
        DataSync.LOGGER.debug("process invite");
        String playerInvited = FTBTeamsAPI.getManager().getInternalPlayerTeam(targetId).playerName;
        Optional<SyncServer> playerLocation = DataSync.playersLocation.getPlayerLocation(playerInvited);
        if(playerLocation.isPresent() && !playerLocation.get().getName().equals(DataSync.serverName)){
            Optional<GameProfile> gameProfile = ServerLifecycleHooks.getCurrentServer().getProfileCache().get(targetId);
            if(gameProfile.isPresent()) {
                try {
                    partyTeam.invite(sourcePlayer, List.of(gameProfile.get()));
                    new SendMessageMessage(Component.translatable("ftbteams.message.invite_sent", sourcePlayer.getName().copy().withStyle(ChatFormatting.YELLOW)), playerInvited).send();
                    Component acceptButton = Component.translatable("ftbteams.accept")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(
                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party join " + partyTeam.getStringID()))
                            );
                    Component declineButton = Component.translatable("ftbteams.decline")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(
                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbteams party deny_invite " + partyTeam.getStringID()))
                            );
                    new SendMessageMessage(Component.literal("[").append(acceptButton).append("] [").append(declineButton).append("]"), playerInvited).send();
                } catch (CommandSyntaxException e) {
                    sourcePlayer.displayClientMessage(Component.literal(e.getMessage()).withStyle(ChatFormatting.RED), false);
                }
            }
            else
                DataSync.LOGGER.error("Could not find GameProfile for player " + playerInvited);
        }

    }
}
