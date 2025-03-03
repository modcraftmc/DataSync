package fr.modcraftmc.datasync.waystones.mixin;

import com.mojang.datafixers.util.Either;
import fr.modcraftmc.crossservercore.api.CrossServerCoreAPI;
import fr.modcraftmc.crossservercore.api.CrossServerCoreProxyExtensionAPI;
import fr.modcraftmc.crossservercore.api.networkdiscovery.ISyncServer;
import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import fr.modcraftmc.datasync.waystones.message.TeleportToWaystone;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.error.WaystoneTeleportError;
import net.blay09.mods.waystones.core.WaystoneTeleportManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = WaystoneTeleportManager.class, remap = false)
public class WaystoneTeleportManagerMixin {

    @Inject(method = "tryTeleport", at = @At("HEAD"), cancellable = true)
    private static void checkServer(WaystoneTeleportContext context, CallbackInfoReturnable<Either<List<Entity>, WaystoneTeleportError>> cir) {
        Entity entity = context.getEntity();
        Waystone waystone = context.getTargetWaystone();

        if (!DatasyncWaystones.waystoneManager.isWaystoneOnCurrentServer(waystone)) {
            if (entity instanceof ServerPlayer serverPlayer) {
                CrossServerCoreAPI.getPlayer(context.getEntity().getUUID()).ifPresent(syncPlayer -> {
                    ISyncServer syncServer = DatasyncWaystones.waystoneManager.getWaystoneServer(waystone);
                    syncServer.sendMessage(new TeleportToWaystone(syncPlayer, waystone.getWaystoneUid()));
                    CrossServerCoreProxyExtensionAPI.transferPlayer(syncPlayer, syncServer);
                });
            }
            cir.setReturnValue(null);
        }
    }
}
