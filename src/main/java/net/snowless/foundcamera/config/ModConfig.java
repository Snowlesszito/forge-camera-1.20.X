package net.snowless.foundcamera.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("foundcamera.json");
    private static Data DATA = new Data();

    private ModConfig() {}

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                Data loaded = GSON.fromJson(r, Data.class);
                if (loaded != null) DATA = loaded;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH)) {
                GSON.toJson(DATA, w);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Data get() {
        return DATA;
    }

    public enum ColorMode {
        REC, VHS, RAW
    }

    public static class Data {
        // Zoom
        public float zoomSpeed = 6.0f;
        public float maxZoom = 4.0f;
        public float zoomStep = 0.25f;
        public boolean zoomWithScroll = true;

        // Efeitos
        public ColorMode colorMode = ColorMode.REC;
        public boolean enableScanlines = false;
        public boolean enableTrackingBand = false;
        public boolean enableGrain = true;
        public boolean enableVignette = true;
        public float grainStrength = 1.2f;
        public float desaturation = 0.65f;
        public float contrast = 1.15f;
        public float vignetteStrength = 0.85f;
        public float infraredTint = 1.0f;
        public float infraredContrast = 1.4f;
        public float infraredEdgeDarkness = 1.3f;

        // Câmera
        public boolean enableCameraSway = true;
        public float swayAmount = 1.0f;
        public float followSpeed = 9.0f;
        public float flipDurationSec = 0.45f;
        public boolean hideVanillaHands = true;

        // Corpo 1ª pessoa
        public float bodyBack = 0.12f;
        public float bodyDown = 0.06f;
        public float bodySide = 0.0f;
        public float armPitch = -1.57f;
        public float armSpread = 0.05f;
        public float freeArmPitch = 0.15f;

        // Tremor de medo
        public boolean fearShakeEnabled = true;
        public float fearShakeIntensity = 1.0f;
        public float fearShakeSpeed = 18.0f;
    }
}