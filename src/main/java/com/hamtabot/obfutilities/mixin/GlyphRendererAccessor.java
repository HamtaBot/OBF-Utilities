package com.hamtabot.obfutilities.mixin;

import net.minecraft.client.font.GlyphRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlyphRenderer.class)
public interface GlyphRendererAccessor {

    @Accessor("minY")
    float obf$getMinY();

    @Accessor("maxY")
    float obf$getMaxY();
}
