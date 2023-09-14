package fr.modcraftmc.datasync.ftbteams.mixin;

import dev.ftb.mods.ftbteams.data.Team;
import fr.modcraftmc.datasync.ftbteams.DatasyncFtbTeam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = dev.ftb.mods.ftbteams.data.Team.class, remap = false)
public class MixinTeam {
    @Inject(at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbteams/data/TeamManager;syncTeamsToAll([Ldev/ftb/mods/ftbteams/data/Team;)V") , method = "denyInvite")
    protected void onDenyInvite(CommandSourceStack source, CallbackInfoReturnable<Integer> cir){
        DatasyncFtbTeam.LOGGER.debug("FTBTeams Team denying invite");
        DatasyncFtbTeam.teamsSynchronizer.syncTeam((dev.ftb.mods.ftbteams.data.Team) (Object) this);
    }

    @Inject(at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbteams/data/Team;getOnlineMembers()Ljava/util/List;") , method = "sendMessage")
    protected void onSendMessage(UUID from, Component text, CallbackInfo ci){
        DatasyncFtbTeam.LOGGER.debug("FTBTeams Team sending message");
        Team team = (Team) (Object) this;
        DatasyncFtbTeam.teamsSynchronizer.syncTeamMessage(team.getId(), from, text);
    }
}
