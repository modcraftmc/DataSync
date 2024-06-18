package fr.modcraftmc.datasync.waystones.mixin;

import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.Waystone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Waystone.class, remap = false)
public class WaystoneMixin {
    @Redirect(method = "write(Lnet/minecraft/network/FriendlyByteBuf;Lnet/blay09/mods/waystones/api/IWaystone;)V", at = @At(value = "INVOKE", target = "Lnet/blay09/mods/waystones/api/IWaystone;getName()Ljava/lang/String;"))
    private static String redirectWriteName(IWaystone instance) {
        DatasyncWaystones.LOGGER.info("Redirecting write name");
        String appendText = DatasyncWaystones.waystoneManager.isWaystoneOnCurrentServer(instance) ? " (l)" : " (r)"; // (l) for local, (r) for remote
        if(instance.getName().isEmpty())
            return instance.getName();
        return instance.getName() + appendText;
    }
}
