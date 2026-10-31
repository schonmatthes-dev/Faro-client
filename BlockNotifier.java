package com.faro.client.module;

import com.faro.client.render.Draw;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public class BlockNotifier extends Module {
    public final Setting.BlockList blocks = add(new Setting.BlockList("Blocks (comma separated)",
            "diamond_ore,deepslate_diamond_ore,ancient_debris"));
    public final Setting.Num duration = add(new Setting.Num("Popup time (s)", 4, 1, 10, 1));

    private record Popup(String title, String sub, long until) {}
    private final List<Popup> popups = new ArrayList<>();
    private final Set<Long> seen = new HashSet<>();

    public BlockNotifier() {
        super("Block Notifier", Category.FINDER, "Popup when a chosen block enters render distance");
    }

    @Override protected void onEnable() { seen.clear(); popups.clear(); }

    @Override public void onTick(Minecraft mc) {
        if (mc.player.tickCount % 20 != 0) return;
        int radius = Math.min(128, mc.options.getEffectiveRenderDistance() * 16);
        Map<Block, List<BlockPos>> fresh = new LinkedHashMap<>();
        for (BlockPos p : BlockScanner.scan(blocks.blocks(), radius, 2000))
            if (seen.add(p.asLong()))
                fresh.computeIfAbsent(mc.level.getBlockState(p).getBlock(), k -> new ArrayList<>()).add(p);
        for (Map.Entry<Block, List<BlockPos>> en : fresh.entrySet()) {
            BlockPos nearest = null; double best = 1e18;
            for (BlockPos p : en.getValue()) {
                double d = mc.player.position().distanceToSqr(p.getCenter());
                if (d < best) { best = d; nearest = p; }
            }
            int n = en.getValue().size();
            popups.add(new Popup(en.getKey().getName().getString() + (n > 1 ? " x" + n : ""),
                    nearest.getX() + " " + nearest.getY() + " " + nearest.getZ() + "  (" + (int) Math.sqrt(best) + "m)",
                    System.currentTimeMillis() + (long) (duration.value * 1000)));
        }
        if (seen.size() > 20000) seen.clear();
    }

    @Override public void onHud(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        popups.removeIf(p -> p.until < now);
        int w = 170, x = (int) Draw.screenW() - w - 8, y = 8;
        for (Popup p : popups) {
            g.fill(x, y, x + w, y + 24, 0xDD1B1226);
            g.fill(x, y, x + 2, y + 24, 0xFFA83BFF);
            g.drawString(mc.font, p.title, x + 8, y + 4, 0xFFEDE6F5, false);
            g.drawString(mc.font, p.sub, x + 8, y + 14, 0xFF8E82A0, false);
            y += 28;
        }
    }
}
