package fr.modcraftmc.datasync.mixins;

import dev.ftb.mods.ftbteams.data.PartyTeam;
import dev.ftb.mods.ftbteams.data.TeamRank;
import dev.ftb.mods.ftbteams.net.PlayerGUIOperationMessage;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.ftbsync.FTBSync;
import net.minecraft.server.level.ServerPlayer;
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
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getPlayerList()Lnet/minecraft/server/players/PlayerList;", ordinal = 1) , method = "processTarget", remap = true)
    protected void onProcessInvite(ServerPlayer sourcePlayer, TeamRank senderRank, PartyTeam partyTeam, UUID targetId, CallbackInfo ci){
        DataSync.LOGGER.debug("process invite");
        FTBSync.inviteInterServer(targetId, partyTeam, sourcePlayer);
    }
}
