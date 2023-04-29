package fr.modcraftmc.datasync.mixins;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbquests.net.CreateObjectMessage;
import fr.modcraftmc.datasync.DataSync;
import fr.modcraftmc.datasync.ftbsync.FTBSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreateObjectMessage.class, remap = false)
public class MixinCreateObjectMessage {
    @Inject(at = @At("TAIL"), method = "handle")
    protected void onHandle(NetworkManager.PacketContext context, CallbackInfo info){
        DataSync.LOGGER.debug("received CreateObjectMessage");
        FTBSync.syncQuests();
    }
}
