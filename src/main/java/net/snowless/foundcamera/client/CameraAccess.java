package net.snowless.foundcamera.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * O Forge 1.20.1 não tem evento para mudar a POSIÇÃO da câmera (só os ângulos).
 * Aqui achamos Camera.setPosition(Vec3) por reflexão, procurando pelo TIPO do parâmetro
 * (e não pelo nome), então funciona igual no dev (Parchment) e no jogo (SRG).
 */
final class CameraAccess {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Method setPosition;
    private static Field positionField;
    private static boolean resolved;
    private static boolean failed;

    private CameraAccess() {}

    static boolean isAvailable() {
        resolve();
        return !failed;
    }

    static void setPosition(Camera camera, Vec3 pos) {
        resolve();
        if (failed) return;
        try {
            if (setPosition != null) {
                setPosition.invoke(camera, pos);
            } else {
                positionField.set(camera, pos);
            }
        } catch (Throwable t) {
            failed = true;
            LOGGER.error("[foundcamera] Falha ao mover a câmera; selfie desativada", t);
        }
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            for (Method m : Camera.class.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == Vec3.class && m.getReturnType() == void.class
                        && !Modifier.isStatic(m.getModifiers())) {
                    m.setAccessible(true);
                    setPosition = m;
                    LOGGER.info("[foundcamera] Camera: usando o metodo {}", m.getName());
                    return;
                }
            }
            // plano B: escrever direto no campo Vec3 "position"
            for (Field f : Camera.class.getDeclaredFields()) {
                if (f.getType() == Vec3.class && !Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    positionField = f;
                    LOGGER.info("[foundcamera] Camera: usando o campo {}", f.getName());
                    return;
                }
            }
            throw new NoSuchMethodException("Camera.setPosition(Vec3) / campo position");
        } catch (Throwable t) {
            failed = true;
            LOGGER.error("[foundcamera] Não achei como mover a câmera; selfie desativada", t);
        }
    }
}
