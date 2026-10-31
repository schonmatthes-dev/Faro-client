package com.faro.client.module;

import com.faro.client.render.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Freecam extends Module {
    public static Freecam INSTANCE;
    public final Setting.Num speed = add(new Setting.Num("Speed (mouse wheel)", 1.0, 0.1, 10.0, 0.1));
    public float yaw, pitch;
    private Vec3 prev = Vec3.ZERO, cur = Vec3.ZERO;

    public Freecam() {
        super("Freecam", Category.CAMERA, "Fly the camera through walls");
        INSTANCE = this;
    }

    @Override protected void onEnable() {
        prev = cur = Draw.camPos();
        yaw = Draw.yaw();
        pitch = Draw.pitch();
    }

    public Vec3 pos(float pt) { return prev.lerp(cur, pt); }

    @Override public void onTick(Minecraft mc) {
        prev = cur;
        Vec3 fwd = Draw.dir(yaw, pitch);
        double yr = Math.toRadians(yaw);
        Vec3 right = new Vec3(-Math.cos(yr), 0, -Math.sin(yr));
        Vec3 mv = Vec3.ZERO;
        if (mc.options.keyUp.isDown()) mv = mv.add(fwd);
        if (mc.options.keyDown.isDown()) mv = mv.subtract(fwd);
        if (mc.options.keyRight.isDown()) mv = mv.add(right);
        if (mc.options.keyLeft.isDown()) mv = mv.subtract(right);
        if (mc.options.keyJump.isDown()) mv = mv.add(0, 1, 0);
        if (mc.options.keyShift.isDown()) mv = mv.add(0, -1, 0);
        if (mv.lengthSqr() > 0) cur = cur.add(mv.normalize().scale(speed.value * 0.4));
    }

    @Override public boolean onScroll(double dy) {
        speed.set(speed.value + dy * 0.2);
        return true;
    }

    @Override public boolean onTurn(double dYaw, double dPitch) {
        yaw += (float) (dYaw * 0.15);
        pitch = Mth.clamp(pitch + (float) (dPitch * 0.15), -90f, 90f);
        return true;
    }
}
