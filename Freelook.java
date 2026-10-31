package com.faro.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class Freelook extends Module {
    public static Freelook INSTANCE;
    public final Setting.Num distance = add(new Setting.Num("Camera distance (mouse wheel)", 4.0, 1.0, 30.0, 0.5));
    public float yaw, pitch;

    public Freelook() {
        super("Freelook", Category.CAMERA, "Look around without turning your player");
        INSTANCE = this;
    }

    @Override protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();
    }

    @Override public boolean onScroll(double dy) {
        distance.set(distance.value - dy * 0.5);
        return true;
    }

    @Override public boolean onTurn(double dYaw, double dPitch) {
        yaw += (float) (dYaw * 0.15);
        pitch = Mth.clamp(pitch + (float) (dPitch * 0.15), -90f, 90f);
        return true;
    }
}
