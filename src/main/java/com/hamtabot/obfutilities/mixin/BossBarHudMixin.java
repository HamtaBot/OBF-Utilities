package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.bossbar.BossBars;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Shadow @Final Map<UUID, ClientBossBar> bossBars;

    @Unique private final Map<UUID, ClientBossBar> obf$allBars = new LinkedHashMap<>();
    @Unique private boolean obf$swapped;

    // the bars redrawn by the mod are taken out for the vanilla pass only, so the others move up into their slot
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"))
    private void obf$hideReplacedBars(DrawContext ctx, CallbackInfo ci) {
        obf$swapped = BossBars.hideReplaced(bossBars, obf$allBars);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("RETURN"))
    private void obf$restoreBars(DrawContext ctx, CallbackInfo ci) {
        if (!obf$swapped) return;
        bossBars.clear();
        bossBars.putAll(obf$allBars);
        obf$allBars.clear();
        obf$swapped = false;
    }
}
