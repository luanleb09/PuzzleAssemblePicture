package com.example.puzzle_assemble_picture;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.xml.KonfettiView;

/**
 * Helper class for celebration effects
 * Provides easy-to-use methods for victory animations
 */
public class CelebrationEffects {

    /**
     * PRESET 1: Full Celebration (Konfetti + Glow + Bounce)
     * Best for: Level completion
     */
    public static void showFullCelebration(
            Context context,
            KonfettiView konfettiView,
            View glowOverlay,
            View scaleView
    ) {
        // Start konfetti
        startKonfetti(konfettiView, KonfettiStyle.PARTY);

        // Glow effect
        startGlowPulse(glowOverlay, 0x40FFD700, 2000);

        // Scale bounce
        startScaleBounce(scaleView, 1.1f, 400);

        // Vibration
        playVictoryVibration(context);
    }

    /**
     * PRESET 2: Simple Celebration (Konfetti only)
     * Best for: Quick achievements
     */
    public static void showSimpleCelebration(
            Context context,
            KonfettiView konfettiView
    ) {
        startKonfetti(konfettiView, KonfettiStyle.BURST);
        playVictoryVibration(context);
    }

    /**
     * Konfetti Styles
     */
    public enum KonfettiStyle {
        PARTY,      // Continuous fall from top
        BURST,      // Single explosion from center
        RAIN,       // Gentle rain effect
        EXPLOSION   // Multiple bursts
    }

    /**
     * Start konfetti with specified style
     */
    public static void startKonfetti(KonfettiView konfettiView, KonfettiStyle style) {
        if (konfettiView == null) return;

        Party party;

        switch (style) {
            case PARTY:
                party = createPartyKonfetti();
                break;
            case BURST:
                party = createBurstKonfetti();
                break;
            case RAIN:
                party = createRainKonfetti();
                break;
            case EXPLOSION:
                party = createExplosionKonfetti();
                break;
            default:
                party = createPartyKonfetti();
        }

        konfettiView.start(party);
    }

    /**
     * Party konfetti - falling from top
     */
    private static Party createPartyKonfetti() {
        EmitterConfig emitterConfig = new Emitter(300, TimeUnit.MILLISECONDS).max(100);

        return new PartyFactory(emitterConfig)
                .spread(360)
                .shapes(Arrays.asList(Shape.Square.INSTANCE, Shape.Circle.INSTANCE))
                .colors(Arrays.asList(
                        0xFFFFD700, // Gold
                        0xFFFF6B6B, // Red
                        0xFF4ECDC4, // Cyan
                        0xFF45B7D1, // Blue
                        0xFFFFA07A, // Orange
                        0xFF98D8C8  // Green
                ))
                .setSpeedBetween(0f, 30f)
                .position(new Position.Relative(0.0, 0.0)
                        .between(new Position.Relative(1.0, 0.0)))
                .build();
    }

    /**
     * Burst konfetti - explosion from center
     */
    private static Party createBurstKonfetti() {
        EmitterConfig emitterConfig = new Emitter(100, TimeUnit.MILLISECONDS).max(50);

        return new PartyFactory(emitterConfig)
                .spread(360)
                .shapes(Arrays.asList(Shape.Square.INSTANCE, Shape.Circle.INSTANCE))
                .colors(Arrays.asList(
                        0xFFFFD700,
                        0xFFFF6B6B,
                        0xFF4ECDC4
                ))
                .setSpeedBetween(10f, 50f)
                .position(new Position.Relative(0.5, 0.5))
                .build();
    }

    /**
     * Rain konfetti - gentle falling
     */
    private static Party createRainKonfetti() {
        EmitterConfig emitterConfig = new Emitter(5000, TimeUnit.MILLISECONDS).max(200);

        return new PartyFactory(emitterConfig)
                .spread(30)
                .shapes(Arrays.asList(Shape.Square.INSTANCE))
                .colors(Arrays.asList(
                        0xFFFFD700,
                        0xFFFFFFFF
                ))
                .setSpeedBetween(5f, 15f)
                .position(new Position.Relative(0.0, 0.0)
                        .between(new Position.Relative(1.0, 0.0)))
                .build();
    }

    /**
     * Explosion konfetti - multiple bursts
     */
    private static Party createExplosionKonfetti() {
        EmitterConfig emitterConfig = new Emitter(50, TimeUnit.MILLISECONDS).max(80);

        return new PartyFactory(emitterConfig)
                .spread(360)
                .shapes(Arrays.asList(Shape.Circle.INSTANCE))
                .colors(Arrays.asList(
                        0xFFFFD700,
                        0xFFFF6B6B,
                        0xFFFFFFFF
                ))
                .setSpeedBetween(20f, 60f)
                .position(new Position.Relative(0.5, 0.5))
                .build();
    }

    /**
     * Glow pulse effect
     */
    public static void startGlowPulse(View glowOverlay, int color, int duration) {
        if (glowOverlay == null) return;

        glowOverlay.setBackgroundColor(color);

        ObjectAnimator glowAnim = ObjectAnimator.ofFloat(
                glowOverlay, "alpha", 0f, 0.6f, 0f, 0.6f, 0f
        );
        glowAnim.setDuration(duration);
        glowAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        glowAnim.start();
    }

    /**
     * Scale bounce effect
     */
    public static void startScaleBounce(View view, float targetScale, int duration) {
        if (view == null) return;

        view.setScaleX(0.8f);
        view.setScaleY(0.8f);

        view.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(duration)
                .setInterpolator(new OvershootInterpolator(2.0f))
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(300)
                            .start();
                })
                .start();
    }

    /**
     * Victory vibration pattern
     */
    public static void playVictoryVibration(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Pattern: short-short-long-short-long (celebration rhythm)
                long[] pattern = {0, 100, 50, 100, 50, 300, 50, 100, 50, 400};
                VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    /**
     * Flash effect
     */
    public static void startFlashEffect(View view, int color) {
        if (view == null) return;

        view.setBackgroundColor(color);
        view.setAlpha(0f);

        view.animate()
                .alpha(0.8f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .start();
                })
                .start();
    }
}