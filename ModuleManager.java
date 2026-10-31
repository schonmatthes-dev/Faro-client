package com.faro.client.module;

import com.faro.client.render.Draw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        MODULES.clear();
        MODULES.add(new Freecam());
        MODULES.add(new Freelook());
        MODULES.add(new EntityEsp());
        MODULES.add(new NameTags());
        MODULES.add(new BlockEsp());
        MODULES.add(new ChunkFinder());
        MODULES.add(new BlockNotifier());
    }

    public static List<Module> all() { return MODULES; }

    public static void tick(Minecraft mc) {
        for (Module m : MODULES) {
            if (!m.isEnabled()) continue;
            if (mc.level == null || mc.player == null) { m.setEnabled(false); continue; }
            m.onTick(mc);
        }
    }

    public static void hud(GuiGraphics g, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        Draw.frame(mc);
        for (Module m : MODULES) if (m.isEnabled()) m.onHud(g, partial);
    }

    public static boolean scroll(double dy) {
        for (Module m : MODULES) if (m.isEnabled() && m.onScroll(dy)) return true;
        return false;
    }

    public static boolean turn(double yaw, double pitch) {
        for (Module m : MODULES) if (m.isEnabled() && m.onTurn(yaw, pitch)) return true;
        return false;
    }
}
