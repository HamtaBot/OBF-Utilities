package com.hamtabot.obfutilities.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DrawContext.class)
public interface DrawContextInvoker {

    /** Flushes the buffers unless a {@code draw(Runnable)} batch is running, like {@code fill} does. */
    @Invoker("tryDraw")
    void obf$tryDraw();
}
