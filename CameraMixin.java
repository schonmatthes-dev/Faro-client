package com.faro.client.mixin;

import com.faro.client.module.Freecam;
import com.faro.client.module.Freelook;
import com.faro.client.render.Draw;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Vec3 position;
    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void faro$setup(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Freecam fc = Freecam.INSTANCE;
        Freelook fl = Freelook.INSTANCE;
        if (fc != null && fc.isEnabled()) {
            this.setPosition(fc.pos(pt));
            this.setRotation(fc.yaw, fc.pitch);
            this.detached = true;
        } else if (fl != null && fl.isEnabled() && mc.player != null) {
            Vec3 eye = mc.player.getEyePosition(pt);
            this.setRotation(fl.yaw, fl.pitch);
            this.setPosition(eye.subtract(Draw.dir(fl.yaw, fl.pitch).scale(fl.distance.value)));
            this.detached = true;
        }
        Draw.setCamera(this.position, this.yRot, this.xRot);
    }
}
