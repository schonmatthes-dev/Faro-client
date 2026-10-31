package com.faro.client.gui;

import com.faro.client.module.Category;
import com.faro.client.module.Module;
import com.faro.client.module.ModuleManager;
import com.faro.client.module.Setting;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ClickGui extends Screen {
    private static final int PANEL = 0xFF1B1226, SIDE = 0xFF150C1D, CARD = 0xFF22162E, ACCENT = 0xFFA83BFF,
            TEXT = 0xFFEDE6F5, DIM = 0xFF8E82A0, LINE = 0xFF3A2A4E, OFF = 0xFF4A4056;

    private static Category selected = Category.CAMERA;
    private static String search = "";

    private static final class Region {
        final int x, y, w, h; final boolean content; final IntConsumer click; final DoubleConsumer drag;
        Region(int x, int y, int w, int h, boolean content, IntConsumer click, DoubleConsumer drag) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.content = content; this.click = click; this.drag = drag;
        }
        boolean hit(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Region> regions = new ArrayList<>();
    private DoubleConsumer dragging;
    private Setting.Text focused;
    private double scroll, curMx;
    private int cx0, cy0, cx1, cy1;

    public ClickGui() { super(Component.literal("Faro Client")); }

    @Override public boolean isPauseScreen() { return false; }

    private void add(int x, int y, int w, int h, boolean content, IntConsumer click, DoubleConsumer drag) {
        regions.add(new Region(x, y, w, h, content, click, drag));
    }

    // ---------- Layout ----------
    private int rowHeight(Setting<?> s) {
        if (s instanceof Setting.Num) return 26;
        if (s instanceof Setting.Text) return 32;
        return 18;
    }

    private int cardHeight(Module m) {
        int h = 28;
        if (m.expanded) { h += 8; for (Setting<?> s : m.settings) h += rowHeight(s); }
        return h;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        regions.clear();
        int W = Math.min(520, width - 16), H = Math.min(310, height - 16);
        int x0 = (width - W) / 2, y0 = (height - H) / 2, sw = 116;

        g.fill(0, 0, width, height, 0x99000000);
        g.fill(x0 - 1, y0 - 1, x0 + W + 1, y0 + H + 1, LINE);
        g.fill(x0, y0, x0 + W, y0 + H, PANEL);
        g.fill(x0, y0, x0 + sw, y0 + H, SIDE);

        // Sidebar
        g.fill(x0 + 12, y0 + 12, x0 + 26, y0 + 26, ACCENT);
        g.fill(x0 + 16, y0 + 16, x0 + 22, y0 + 22, SIDE);
        g.drawString(font, Component.literal("FARO").withStyle(ChatFormatting.BOLD), x0 + 32, y0 + 11, TEXT, false);
        g.drawString(font, "CLIENT", x0 + 32, y0 + 21, DIM, false);
        g.drawString(font, "MODULES", x0 + 12, y0 + 44, DIM, false);
        int cy = y0 + 58;
        for (Category c : Category.values()) {
            boolean sel = c == selected && search.isEmpty();
            if (sel) { g.fill(x0 + 6, cy - 3, x0 + sw - 6, cy + 13, 0xFF35204A); g.fill(x0 + 6, cy - 3, x0 + 8, cy + 13, ACCENT); }
            g.drawString(font, c.label, x0 + 16, cy + 1, sel ? TEXT : DIM, false);
            final Category cc = c;
            add(x0 + 6, cy - 3, sw - 12, 16, false, b -> { selected = cc; search = ""; scroll = 0; }, null);
            cy += 20;
        }
        long on = ModuleManager.all().stream().filter(Module::isEnabled).count();
        g.fill(x0 + 6, y0 + H - 36, x0 + sw - 6, y0 + H - 6, 0xFF1D1228);
        g.drawString(font, "Faro Client", x0 + 12, y0 + H - 31, TEXT, false);
        g.drawString(font, on + " / " + ModuleManager.all().size() + " active", x0 + 12, y0 + H - 20, DIM, false);

        // Top bar
        int ax = x0 + sw + 8, aw = W - sw - 16;
        g.fill(ax, y0 + 8, ax + aw, y0 + 28, SIDE);
        g.fill(ax, y0 + 27, ax + aw, y0 + 28, ACCENT);
        g.drawString(font, search.isEmpty() ? selected.label + " modules" : "Search results", ax + 8, y0 + 14, TEXT, false);
        int bx = ax + aw - 150;
        g.fill(bx, y0 + 11, bx + 126, y0 + 25, 0xFF0F0817);
        g.drawString(font, search.isEmpty() ? "Search modules..." : search + "_", bx + 4, y0 + 15, search.isEmpty() ? DIM : TEXT, false);
        g.drawString(font, "x", ax + aw - 12, y0 + 14, TEXT, false);
        add(ax + aw - 18, y0 + 8, 18, 20, false, b -> onClose(), null);

        // Footer
        g.drawString(font, "Left click: toggle / Right click: settings", ax + 2, y0 + H - 14, DIM, false);
        g.drawString(font, "ESC Close", ax + aw - font.width("ESC Close"), y0 + H - 14, DIM, false);

        // Module list
        cx0 = ax; cy0 = y0 + 34; cx1 = ax + aw; cy1 = y0 + H - 22;
        String q = search.toLowerCase();
        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            boolean ok = !q.isEmpty() ? m.name.toLowerCase().contains(q)
                    : selected == Category.FAVORITES ? m.favorite : m.category == selected;
            if (ok) list.add(m);
        }
        int colW = (aw - 6) / 2;
        int[] col = new int[list.size()], ys = new int[list.size()], colH = {0, 0};
        for (int i = 0; i < list.size(); i++) {
            int c = colH[0] <= colH[1] ? 0 : 1;
            col[i] = c; ys[i] = colH[c];
            colH[c] += cardHeight(list.get(i)) + 6;
        }
        int maxScroll = Math.max(0, Math.max(colH[0], colH[1]) - (cy1 - cy0));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        g.enableScissor(cx0, cy0, cx1, cy1);
        if (list.isEmpty())
            g.drawString(font, selected == Category.FAVORITES && q.isEmpty() ? "No favorites yet - click the star on a module" : "No modules found",
                    cx0 + 4, cy0 + 6, DIM, false);
        for (int i = 0; i < list.size(); i++) {
            int x = cx0 + col[i] * (colW + 6), y = cy0 - (int) scroll + ys[i];
            Module m = list.get(i);
            int h = cardHeight(m);
            if (y > cy1 || y + h < cy0) continue;
            drawCard(g, m, x, y, colW, h);
        }
        g.disableScissor();
    }

    private void drawToggle(GuiGraphics g, int x, int y, boolean on) {
        g.fill(x, y, x + 22, y + 10, on ? ACCENT : OFF);
        int kx = on ? x + 13 : x + 1;
        g.fill(kx, y + 1, kx + 8, y + 9, 0xFFFFFFFF);
    }

    private void drawCard(GuiGraphics g, Module m, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, CARD);
        if (m.expanded) Draw_outline(g, x, y, x + w, y + h);
        g.drawString(font, m.name, x + 8, y + 5, TEXT, false);
        g.drawString(font, font.plainSubstrByWidth(m.description, w - 70), x + 8, y + 16, DIM, false);
        g.drawString(font, m.favorite ? "\u2605" : "\u2606", x + w - 46, y + 9, m.favorite ? ACCENT : DIM, false);
        drawToggle(g, x + w - 30, y + 9, m.isEnabled());

        add(x, y, w, 28, true, b -> { if (b == 0) m.toggle(); else if (b == 1) m.expanded = !m.expanded; }, null);
        add(x + w - 50, y, 16, 28, true, b -> { if (b == 0) m.favorite = !m.favorite; }, null);
        if (!m.expanded) return;

        int sy = y + 34;
        int tx = x + 8, tw = w - 16;
        for (Setting<?> s : m.settings) {
            if (s instanceof Setting.Bool b) {
                g.drawString(font, font.plainSubstrByWidth(b.name, tw - 30), tx, sy + 1, TEXT, false);
                drawToggle(g, x + w - 30, sy, b.value);
                add(x + 4, sy - 2, w - 8, 16, true, k -> b.value = !b.value, null);
            } else if (s instanceof Setting.Num n) {
                g.drawString(font, font.plainSubstrByWidth(n.name, tw - 34), tx, sy, TEXT, false);
                String v = n.display();
                g.drawString(font, v, tx + tw - font.width(v), sy, DIM, false);
                g.fill(tx, sy + 14, tx + tw, sy + 16, LINE);
                int fw = (int) (tw * n.fraction());
                g.fill(tx, sy + 14, tx + fw, sy + 16, ACCENT);
                g.fill(tx + fw - 2, sy + 11, tx + fw + 2, sy + 19, 0xFFFFFFFF);
                add(tx - 2, sy + 8, tw + 4, 14, true, k -> n.setFraction((curMx - tx) / tw), d -> n.setFraction((d - tx) / tw));
            } else if (s instanceof Setting.Color c) {
                g.drawString(font, c.name, tx, sy + 1, TEXT, false);
                int bw = 56, bx = tx + tw - bw;
                g.fill(bx, sy - 2, bx + bw, sy + 12, 0xFF0F0817);
                g.fill(bx + 3, sy + 1, bx + 11, sy + 9, 0xFF000000 | c.value);
                g.drawString(font, c.hex(), bx + 14, sy + 2, TEXT, false);
                add(bx, sy - 2, bw, 14, true, k -> c.cycle(k == 1 ? -1 : 1), null);
            } else if (s instanceof Setting.Text t) {
                g.drawString(font, t.name, tx, sy, TEXT, false);
                boolean f = focused == t;
                g.fill(tx, sy + 10, tx + tw, sy + 24, 0xFF0F0817);
                if (f) Draw_outline(g, tx, sy + 10, tx + tw, sy + 24);
                String shown = f ? font.plainSubstrByWidth(t.value + "_", tw - 6, true) : font.plainSubstrByWidth(t.value, tw - 6);
                g.drawString(font, shown, tx + 3, sy + 14, f ? TEXT : DIM, false);
                add(tx, sy + 10, tw, 14, true, k -> focused = t, null);
            }
            sy += rowHeight(s);
        }
    }

    private void Draw_outline(GuiGraphics g, int x1, int y1, int x2, int y2) {
        com.faro.client.render.Draw.outline(g, x1, y1, x2, y2, ACCENT);
    }

    // ---------- Input ----------
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        double mx = e.x(), my = e.y();
        focused = null;
        curMx = mx;
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region r = regions.get(i);
            if (!r.hit(mx, my)) continue;
            if (r.content && !(mx >= cx0 && mx < cx1 && my >= cy0 && my < cy1)) continue;
            r.click.accept(e.button());
            dragging = r.drag;
            return true;
        }
        return super.mouseClicked(e, dbl);
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging != null) { dragging.accept(e.x()); return true; }
        return super.mouseDragged(e, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent e) {
        dragging = null;
        return super.mouseReleased(e);
    }

    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll -= sy * 18;
        return true;
    }

    @Override public boolean keyPressed(KeyEvent e) {
        int key = e.key();
        if (focused != null) {
            if (key == 259 && !focused.value.isEmpty()) focused.value = focused.value.substring(0, focused.value.length() - 1);
            else if (key == 257 || key == 256 || key == 335) focused = null;
            else if (key == 344) onClose();
            return true;
        }
        if (key == 344) { onClose(); return true; }               // Right Shift schliesst wieder
        if (key == 259 && !search.isEmpty()) { search = search.substring(0, search.length() - 1); return true; }
        return super.keyPressed(e);
    }

    @Override public boolean charTyped(CharacterEvent e) {
        String s = Character.toString(e.codepoint());
        if (focused != null) { if (focused.value.length() < 200) focused.value += s; return true; }
        if (search.length() < 20 && !Character.isISOControl(e.codepoint())) { search += s; scroll = 0; }
        return true;
    }
}
