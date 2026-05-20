package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ClientPlayNetworkHandler.class)
public class StackKillMixin {

    private static final Map<Integer, Integer> stackCounts = new HashMap<>();
    private static final Pattern STACK_PATTERN = Pattern.compile("x(\\d+)");
    private static int ticksSinceCleanup = 0;

    private static final double MAX_DISTANCE_SQUARED = 4.0 * 4.0;

    @Inject(method = "onEntityTrackerUpdate", at = @At("TAIL"))
    private void onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        ticksSinceCleanup++;
        if (ticksSinceCleanup >= 200) {
            ticksSinceCleanup = 0;
            stackCounts.keySet().removeIf(id -> client.world.getEntityById(id) == null);
        }

        Entity entity = client.world.getEntityById(packet.id());
        if (!(entity instanceof LivingEntity) || entity instanceof PlayerEntity) return;

        List<DataTracker.SerializedEntry<?>> entries = packet.trackedValues();
        if (entries == null) return;

        for (DataTracker.SerializedEntry<?> entry : entries) {
            Object value = entry.value();
            if (!(value instanceof Optional)) continue;
            Optional<?> opt = (Optional<?>) value;
            if (opt.isEmpty() || !(opt.get() instanceof Text)) continue;

            String name = ((Text) opt.get()).getString();
            Matcher matcher = STACK_PATTERN.matcher(name);
            if (!matcher.find()) continue;

            int newCount;
            try {
                newCount = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) { continue; }

            int entityId = entity.getId();
            Integer prevCount = stackCounts.get(entityId);

            if (prevCount != null && newCount < prevCount) {
                int killed = prevCount - newCount;
                if (client.player.squaredDistanceTo(entity) < MAX_DISTANCE_SQUARED) {
                    OBFUtilities.onMobKilled(killed);
                }
            }

            stackCounts.put(entityId, newCount);
        }
    }
}