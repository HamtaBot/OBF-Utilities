package com.hamtabot.obfutilities.mixin;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {

    @Accessor("image")
    NativeImage obf$getImage();

    @Accessor("mipmapLevelsImages")
    NativeImage[] obf$getMipmapLevelsImages();

    @Mutable
    @Accessor("mipmapLevelsImages")
    void obf$setMipmapLevelsImages(NativeImage[] images);
}
