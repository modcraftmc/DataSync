package fr.modcraftmc.datasync.waystones.mixin;

import fr.modcraftmc.crossservercoreapi.CrossServerCoreAPI;
import net.blay09.mods.waystones.network.message.EditWaystoneMessage;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EditWaystoneMessage.class, remap = false)
public class EditWaystoneMessageMixin {

    @Inject(method = "makeNameLegal", at = @At(value = "RETURN"), cancellable = true)
    private static void addServerName(MinecraftServer server, String name, CallbackInfoReturnable<String> cir) {
        String serverName = CrossServerCoreAPI.instance.getServerName();
        if (!cir.getReturnValue().startsWith(serverName + ": ")) {
            cir.setReturnValue(serverName + ": " + cir.getReturnValue());
        }
    }
}
