package net.snowless.foundcamera.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ModConfigScreen {
    private ModConfigScreen() {}

    public static Screen create(Screen parent) {
        ModConfig.Data d = ModConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.foundcamera.title"))
                .setSavingRunnable(ModConfig::save);
        ConfigEntryBuilder entry = builder.entryBuilder();

        // Zoom
        ConfigCategory zoom = builder.getOrCreateCategory(Component.translatable("config.foundcamera.category.zoom"));
        zoom.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.zoom_speed"), d.zoomSpeed)
                .setDefaultValue(6f).setMin(0.5f).setMax(30f)
                .setTooltip(Component.translatable("config.foundcamera.zoom_speed.tooltip"))
                .setSaveConsumer(v -> d.zoomSpeed = v).build());
        zoom.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.max_zoom"), d.maxZoom)
                .setDefaultValue(4f).setMin(1.5f).setMax(12f)
                .setSaveConsumer(v -> d.maxZoom = v).build());
        zoom.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.zoom_step"), d.zoomStep)
                .setDefaultValue(0.25f).setMin(0.05f).setMax(1f)
                .setSaveConsumer(v -> d.zoomStep = v).build());
        zoom.addEntry(entry.startBooleanToggle(Component.translatable("config.foundcamera.zoom_scroll"), d.zoomWithScroll)
                .setDefaultValue(true).setSaveConsumer(v -> d.zoomWithScroll = v).build());

        // Effects
        ConfigCategory fx = builder.getOrCreateCategory(Component.translatable("config.foundcamera.category.fx"));
        fx.addEntry(entry.startEnumSelector(Component.translatable("config.foundcamera.color_mode"),
                        ModConfig.ColorMode.class, d.colorMode)
                .setDefaultValue(ModConfig.ColorMode.REC)
                .setEnumNameProvider(e -> Component.translatable("config.foundcamera.color_mode." + e.name().toLowerCase()))
                .setSaveConsumer(v -> {
                    d.colorMode = v;
                    if (v == ModConfig.ColorMode.REC) {
                        d.enableScanlines = false;
                        d.enableTrackingBand = false;
                        d.enableGrain = true;
                        d.desaturation = 0.65f;
                        d.grainStrength = 1.2f;
                    } else if (v == ModConfig.ColorMode.VHS) {
                        d.enableScanlines = true;
                        d.enableTrackingBand = true;
                        d.enableGrain = true;
                        d.desaturation = 0.25f;
                        d.grainStrength = 1.0f;
                    } else {
                        d.enableScanlines = false;
                        d.enableTrackingBand = false;
                        d.enableGrain = true;
                        d.desaturation = 0.15f;
                        d.grainStrength = 0.5f;
                    }
                }).build());
        fx.addEntry(entry.startBooleanToggle(Component.translatable("config.foundcamera.enable_scanlines"), d.enableScanlines)
                .setDefaultValue(false).setSaveConsumer(v -> d.enableScanlines = v).build());
        fx.addEntry(entry.startBooleanToggle(Component.translatable("config.foundcamera.enable_tracking"), d.enableTrackingBand)
                .setDefaultValue(false).setSaveConsumer(v -> d.enableTrackingBand = v).build());
        fx.addEntry(entry.startBooleanToggle(Component.translatable("config.foundcamera.enable_grain"), d.enableGrain)
                .setDefaultValue(true).setSaveConsumer(v -> d.enableGrain = v).build());
        fx.addEntry(entry.startBooleanToggle(Component.translatable("config.foundcamera.enable_vignette"), d.enableVignette)
                .setDefaultValue(true).setSaveConsumer(v -> d.enableVignette = v).build());
        fx.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.grain"), d.grainStrength)
                .setDefaultValue(1.2f).setMin(0f).setMax(3f).setSaveConsumer(v -> d.grainStrength = v).build());
        fx.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.desaturation"), d.desaturation)
                .setDefaultValue(0.65f).setMin(0f).setMax(1f)
                .setTooltip(Component.translatable("config.foundcamera.desaturation.tooltip"))
                .setSaveConsumer(v -> d.desaturation = v).build());
        fx.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.contrast"), d.contrast)
                .setDefaultValue(1.15f).setMin(0.5f).setMax(2f).setSaveConsumer(v -> d.contrast = v).build());
        fx.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.vignette"), d.vignetteStrength)
                .setDefaultValue(0.85f).setMin(0f).setMax(2f).setSaveConsumer(v -> d.vignetteStrength = v).build());
        fx.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.ir_tint"), d.infraredTint)
                .setDefaultValue(1f).setMin(0f).setMax(2f).setSaveConsumer(v -> d.infraredTint = v).build());
        fx.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.ir_contrast"), d.infraredContrast)
                .setDefaultValue(1.4f).setMin(1f).setMax(2.5f).setSaveConsumer(v -> d.infraredContrast = v).build());
        fx.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.ir_edge"), d.infraredEdgeDarkness)
                .setDefaultValue(1.3f).setMin(0.5f).setMax(2.5f).setSaveConsumer(v -> d.infraredEdgeDarkness = v).build());

        // Camera & body
        ConfigCategory cam = builder.getOrCreateCategory(Component.translatable("config.foundcamera.category.cam"));
        cam.addEntry(entry.startBooleanToggle(Component.translatable("config.foundcamera.enable_sway"), d.enableCameraSway)
                .setDefaultValue(true).setSaveConsumer(v -> d.enableCameraSway = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.sway_amount"), d.swayAmount)
                .setDefaultValue(1f).setMin(0f).setMax(3f).setSaveConsumer(v -> d.swayAmount = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.follow_speed"), d.followSpeed)
                .setDefaultValue(9f).setMin(1f).setMax(30f)
                .setTooltip(Component.translatable("config.foundcamera.follow_speed.tooltip"))
                .setSaveConsumer(v -> d.followSpeed = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.flip_duration"), d.flipDurationSec)
                .setDefaultValue(0.45f).setMin(0.15f).setMax(1.5f).setSaveConsumer(v -> d.flipDurationSec = v).build());
        cam.addEntry(entry.startBooleanToggle(Component.translatable("config.foundcamera.hide_hands"), d.hideVanillaHands)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.foundcamera.hide_hands.tooltip"))
                .setSaveConsumer(v -> d.hideVanillaHands = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.body_back"), d.bodyBack)
                .setDefaultValue(0.04f).setMin(-0.5f).setMax(0.5f)
                .setTooltip(Component.translatable("config.foundcamera.body_back.tooltip"))
                .setSaveConsumer(v -> d.bodyBack = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.body_down"), d.bodyDown)
                .setDefaultValue(0.06f).setMin(-0.5f).setMax(0.5f)
                .setTooltip(Component.translatable("config.foundcamera.body_down.tooltip"))
                .setSaveConsumer(v -> d.bodyDown = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.body_side"), d.bodySide)
                .setDefaultValue(0f).setMin(-0.5f).setMax(0.5f)
                .setTooltip(Component.translatable("config.foundcamera.body_side.tooltip"))
                .setSaveConsumer(v -> d.bodySide = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.arm_pitch"), d.armPitch)
                .setDefaultValue(-1.57f).setMin(-1.8f).setMax(0.5f)
                .setTooltip(Component.translatable("config.foundcamera.arm_pitch.tooltip"))
                .setSaveConsumer(v -> d.armPitch = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.arm_spread"), d.armSpread)
                .setDefaultValue(0.05f).setMin(0f).setMax(0.8f)
                .setTooltip(Component.translatable("config.foundcamera.arm_spread.tooltip"))
                .setSaveConsumer(v -> d.armSpread = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.free_arm_pitch"), d.freeArmPitch)
                .setDefaultValue(0.15f).setMin(-1.5f).setMax(1f)
                .setSaveConsumer(v -> d.freeArmPitch = v).build());
        cam.addEntry(entry.startBooleanToggle(Component.translatable("config.foundcamera.fear_shake"), d.fearShakeEnabled)
                .setDefaultValue(true).setSaveConsumer(v -> d.fearShakeEnabled = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.fear_intensity"), d.fearShakeIntensity)
                .setDefaultValue(1f).setMin(0f).setMax(3f).setSaveConsumer(v -> d.fearShakeIntensity = v).build());
        cam.addEntry(entry.startFloatField(Component.translatable("config.foundcamera.fear_speed"), d.fearShakeSpeed)
                .setDefaultValue(18f).setMin(1f).setMax(40f).setSaveConsumer(v -> d.fearShakeSpeed = v).build());

        return builder.build();
    }
}