package com.hamtabot.obfutilities.mixin;

import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.font.BitmapFont$BitmapFontGlyph")
public interface BitmapGlyphAccessor {

    @Accessor("scaleFactor") float obf$getScaleFactor();

    @Accessor("image") NativeImage obf$getImage();

    @Accessor("x") int obf$getX();

    @Accessor("y") int obf$getY();

    @Accessor("width") int obf$getWidth();

    @Accessor("height") int obf$getHeight();

    @Accessor("ascent") int obf$getAscent();
}
