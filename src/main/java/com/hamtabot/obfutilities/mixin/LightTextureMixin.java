package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightmapTextureManager.class)
public class LightTextureMixin {

    // Intercepte le .floatValue() sur le Double retourné par getGamma().getValue()
    // C'est exactement ce que fait le full bright que j'ai trouvé sur le grand internet web
    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Double;floatValue()F",
                    ordinal = 1
            )
    )
    private float redirectGammaFloat(Double instance) {
        if (OBFUtilities.config.fullBrightEnabled) {
            return (float) OBFUtilities.config.fullBrightLevel;
        }
        return instance.floatValue();
    }
}