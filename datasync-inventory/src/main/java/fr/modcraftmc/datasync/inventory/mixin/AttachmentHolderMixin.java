package fr.modcraftmc.datasync.inventory.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AttachmentHolder.class)
public interface AttachmentHolderMixin {

    @Invoker("deserializeAttachments")
    public void datasync_deserializeAttachments(HolderLookup.Provider provider, CompoundTag tag);
}
