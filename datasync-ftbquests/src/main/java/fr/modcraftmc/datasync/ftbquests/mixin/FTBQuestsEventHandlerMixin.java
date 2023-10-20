package fr.modcraftmc.datasync.ftbquests.mixin;

import dev.ftb.mods.ftbquests.FTBQuestsEventHandler;
import fr.modcraftmc.datasync.ftbquests.DatasyncFtbQuests;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FTBQuestsEventHandler.class, remap = false)
public class FTBQuestsEventHandlerMixin {
    @Inject(method = "serverStarted", at = @At("TAIL"))
    public void onFtbQuestsLoadedData(MinecraftServer server, CallbackInfo ci){
        DatasyncFtbQuests.questsSynchronizer.loadTeamsQuests();
    }
}
