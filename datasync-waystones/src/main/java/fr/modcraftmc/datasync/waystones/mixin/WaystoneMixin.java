package fr.modcraftmc.datasync.waystones.mixin;

import fr.modcraftmc.datasync.waystones.DatasyncWaystones;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.core.WaystoneImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WaystoneImpl.class, remap = false)
public class WaystoneMixin {
    //redirect is not working i don't know why
    @Redirect(method = "write(Lnet/minecraft/network/FriendlyByteBuf/RegistryFriendlyByteBuf;Lnet/blay09/mods/waystones/api/Waystone;)V", at = @At(value = "INVOKE", target = "Lnet/blay09/mods/waystones/core/WaystoneImpl;getName()Lnet/minecraft/network/chat/Component;"), remap = false)
    private static Component redirectWriteName(Waystone instance) {
        String appendText = DatasyncWaystones.waystoneManager.isWaystoneOnCurrentServer(instance) ? " (l)" : " (r)"; // (l) for local, (r) for remote
        if(instance.getName().getString().isEmpty())
            return instance.getName();

        MutableComponent name = instance.getName().copy();
        name.append(appendText);
        return name;
    }
}
