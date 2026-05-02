package io.github.zhengzhengyiyi.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Memory optimisation mixin targeting Minecraft.class.
 *
 * Three injections, one class:
 *
 *  1. setLevel (HEAD)   — clear stale particles + GC before the new world
 *                         starts allocating, flattening the join-world RAM spike.
 *  2. clearClientLevel (RETURN) — GC after all old-world references are nulled,
 *                         so the JVM reclaims chunk/entity memory promptly on
 *                         disconnect instead of waiting for the next GC pressure.
 */
@Mixin(Minecraft.class)
public class MinecraftMemoryMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("config_editor/Memory");

    @Shadow @Final
    public ParticleEngine particleEngine;

    // -------------------------------------------------------------------------
    // 1. Before a new level is set: clear particles + hint GC
    // -------------------------------------------------------------------------
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void onSetLevel(ClientLevel level, CallbackInfo ci) {
        // Drop stale particle objects from the old world so they don't hold
        // old block/sound references during the new world's allocation phase.
        if (particleEngine != null) {
            particleEngine.clearParticles();
        }

        // Hint the JVM to collect now, while we are on the loading screen.
        // This is the natural pause where a GC pause is invisible to the player.
        long before = usedMemoryMb();
        System.gc();
        long after = usedMemoryMb();
        LOGGER.info("World load: cleared particles + GC freed ~{} MB (heap now {} MB)",
                before - after, after);
    }

    // -------------------------------------------------------------------------
    // 2. After the old level is fully cleared: hint GC again
    // -------------------------------------------------------------------------
    @Inject(method = "clearClientLevel", at = @At("RETURN"))
    private void onClearClientLevel(Screen screen, CallbackInfo ci) {
        // All old-level references (chunks, entities, render buffers) have been
        // nulled by this point, so the GC has the maximum amount to collect.
        long before = usedMemoryMb();
        System.gc();
        long after = usedMemoryMb();
        LOGGER.info("World clear: GC freed ~{} MB (heap now {} MB)",
                before - after, after);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------
    private static long usedMemoryMb() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }
}
