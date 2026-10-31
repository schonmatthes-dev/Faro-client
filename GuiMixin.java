package com.faro.client.mixin;

import com.faro.client.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void faro$render(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        ModuleManager.hud(graphics, tracker.getGameTimeDeltaPartialTick(false));
    }
}
