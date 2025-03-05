package fr.modcraftmc.datasync.ftbteams.mixin;

import dev.ftb.mods.ftbteams.api.Team;
import fr.modcraftmc.datasync.ftbteams.DatasyncFtbTeam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = dev.ftb.mods.ftbteams.data.AbstractTeam.class, remap = false)
public class MixinAbstractTeam {
    @Inject(at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbteams/data/TeamManagerImpl;syncToAll([Ldev/ftb/mods/ftbteams/api/Team;)V") , method = "declineInvitation")
    protected void onDenyInvite(CommandSourceStack source, CallbackInfoReturnable<Integer> cir){
        DatasyncFtbTeam.LOGGER.debug("FTBTeams Team denying invite");
        DatasyncFtbTeam.teamsSynchronizer.syncTeam((dev.ftb.mods.ftbteams.api.Team) (Object) this);
    }

    @Inject(at = @At(value = "RETURN"), method = "sendMessage(Ljava/util/UUID;Lnet/minecraft/network/chat/Component;)V")
    protected void onSendMessage(UUID from, Component text, CallbackInfo ci){
        DatasyncFtbTeam.LOGGER.debug("FTBTeams Team sending message");
        Team team = (Team) (Object) this;
        DatasyncFtbTeam.teamsSynchronizer.syncTeamMessage(team.getId(), from, text);
    }
}
