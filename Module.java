package com.faro.client.module;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public abstract class Module {
    public final String name, description;
    public final Category category;
    public final List<Setting<?>> settings = new ArrayList<>();
    public boolean favorite, expanded;
    private boolean enabled;

    protected Module(String name, Category category, String description) {
        this.name = name; this.category = category; this.description = description;
    }

    protected <S extends Setting<?>> S add(S s) { settings.add(s); return s; }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }
    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        if (e) onEnable(); else onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick(Minecraft mc) {}
    public void onHud(GuiGraphics g, float partial) {}
    /** true = Scroll verbraucht */
    public boolean onScroll(double dy) { return false; }
    /** true = Mausbewegung verbraucht (Spieler dreht sich nicht) */
    public boolean onTurn(double dYaw, double dPitch) { return false; }
}
