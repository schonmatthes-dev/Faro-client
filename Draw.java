package com.faro.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 2D-Overlay-Rendering: Weltpunkte werden selbst auf den Bildschirm projiziert
 * und mit GuiGraphics gezeichnet. Dadurch sieht man alles durch Waende und die
 * Mod haengt nicht an der (haeufig geaenderten) 3D-Render-Pipeline.
 */
public final class Draw {
    private static Vec3 cam = Vec3.ZERO;
    private static float yaw, pitch;
    private static double fx, fy, fz, rx, rz, ux, uy, uz, sw, sh, focal;

    public static void setCamera(Vec3 p, float y, float pi) { cam = p; yaw = y; pitch = pi; }
    public static Vec3 camPos() { return cam; }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    public static Vec3 dir(float yawDeg, float pitchDeg) {
        double yr = Math.toRadians(yawDeg), pr = Math.toRadians(pitchDeg);
        return new Vec3(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
    }

    public static void frame(Minecraft mc) {
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        fx = -Math.sin(yr) * Math.cos(pr); fy = -Math.sin(pr); fz = Math.cos(yr) * Math.cos(pr);
        rx = -Math.cos(yr); rz = -Math.sin(yr);
        ux = -rz * fy; uy = rz * fx - rx * fz; uz = rx * fy;
        sw = mc.getWindow().getGuiScaledWidth();
        sh = mc.getWindow().getGuiScaledHeight();
        focal = (sh / 2) / Math.tan(Math.toRadians(mc.options.fov().get()) / 2);
    }

    public static double screenW() { return sw; }
    public static double screenH() { return sh; }

    private static double[] toCam(Vec3 w) {
        double dx = w.x - cam.x, dy = w.y - cam.y, dz = w.z - cam.z;
        return new double[]{dx * rx + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
    }

    private static double[] scr(double[] c) {
        return new double[]{sw / 2 + c[0] / c[2] * focal, sh / 2 - c[1] / c[2] * focal};
    }

    /** Bildschirmposition (GUI-Koordinaten) oder null, wenn hinter der Kamera. */
    public static double[] project(Vec3 w) {
        double[] c = toCam(w);
        return c[2] < 0.05 ? null : scr(c);
    }

    public static void line(GuiGraphics g, double x1, double y1, double x2, double y2, int color) {
        double dx = x2 - x1, dy = y2 - y1;
        int n = (int) Math.min(3000, Math.max(Math.abs(dx), Math.abs(dy)));
        if (n <= 0) n = 1;
        for (int i = 0; i <= n; i++) {
            int px = (int) Math.round(x1 + dx * i / n), py = (int) Math.round(y1 + dy * i / n);
            if (px < 0 || py < 0 || px >= sw || py >= sh) continue;
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    public static void line3D(GuiGraphics g, Vec3 a, Vec3 b, int color) {
        double[] ca = toCam(a), cb = toCam(b);
        double n = 0.05;
        if (ca[2] < n && cb[2] < n) return;
        if (ca[2] < n) ca = clip(ca, cb, n); else if (cb[2] < n) cb = clip(cb, ca, n);
        double[] sa = scr(ca), sb = scr(cb);
        line(g, sa[0], sa[1], sb[0], sb[1], color);
    }

    private static double[] clip(double[] bad, double[] good, double n) {
        double t = (n - bad[2]) / (good[2] - bad[2]);
        return new double[]{bad[0] + (good[0] - bad[0]) * t, bad[1] + (good[1] - bad[1]) * t, n};
    }

    public static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    /** Zeichnet den 2D-Rahmen um eine Box. Gibt {minX,minY,maxX,maxY} zurueck oder null. */
    public static int[] box(GuiGraphics g, AABB b, int color) {
        double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
        for (int i = 0; i < 8; i++) {
            double[] p = project(new Vec3((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY,
                    (i & 4) == 0 ? b.minZ : b.maxZ));
            if (p == null) return null;
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
        }
        if (maxX < 0 || maxY < 0 || minX > sw || minY > sh) return null;
        int[] r = {(int) minX, (int) minY, (int) maxX, (int) maxY};
        outline(g, r[0], r[1], Math.max(r[2], r[0] + 2), Math.max(r[3], r[1] + 2), color);
        return r;
    }

    /** Tracer von unten Mitte zum Ziel (auch wenn das Ziel hinter einem liegt). */
    public static void tracer(GuiGraphics g, Vec3 target, int color) {
        double[] c = toCam(target);
        double ox = sw / 2, oy = sh;
        if (c[2] >= 0.05) {
            double[] s = scr(c);
            line(g, ox, oy, s[0], s[1], color);
        } else {
            double dx = c[0], dy = -c[1], len = Math.hypot(dx, dy);
            if (len < 1e-6) return;
            line(g, ox, oy, sw / 2 + dx / len * sw, sh / 2 + dy / len * sw, color);
        }
    }
}
