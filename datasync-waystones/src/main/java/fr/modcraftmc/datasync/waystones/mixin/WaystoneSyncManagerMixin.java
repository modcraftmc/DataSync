package fr.modcraftmc.datasync.waystones.mixin;

import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.datasync.waystones.message.UpdateWaystone;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.core.WaystoneSyncManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WaystoneSyncManager.class, remap = false)
public class WaystoneSyncManagerMixin {

    @Inject(method = "sendWaystoneUpdateToAll", at = @At("RETURN"))
    private static void sendMessage(MinecraftServer server, IWaystone waystone, CallbackInfo ci) {
        CrossServerCoreAPI.instance.sendCrossMessageToAllOtherServer(new UpdateWaystone(waystone));
    }
}
