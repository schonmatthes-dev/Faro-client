package com.faro.client.module;

import com.faro.client.render.Draw;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

/**
 * Markiert Chunks mit Amethyst-Geoden oder auffaellig vielen "Spieler-Bloecken"
 * (Kisten, Ofen, Betten, Trichter, ...). Es koennen nur Chunks gescannt werden,
 * die dein Client geladen hat (= Server-Renderdistanz).
 */
public class ChunkFinder extends Module {
    public final Setting.Num simDistance = add(new Setting.Num("Simulation distance", 9, 2, 32, 1));
    public final Setting.Num sensitivity = add(new Setting.Num("Sensitivity", 3, 1, 10, 1));
    public final Setting.Num alpha = add(new Setting.Num("Alpha", 128, 20, 255, 1));
    public final Setting.Color color = add(new Setting.Color("Highlight color", 0xC447FF));
    public final Setting.Bool geodes = add(new Setting.Bool("Amethyst geodes", true));
    public final Setting.Bool activity = add(new Setting.Bool("Player activity", true));

    private record Hit(int cx, int cz, String label, double y) {}
    private List<Hit> hits = new ArrayList<>();

    public ChunkFinder() {
        super("Chunk Finder", Category.FINDER, "Find geodes & chunks with player activity");
    }

    @Override protected void onEnable() { hits = new ArrayList<>(); scan(Minecraft.getInstance()); }

    @Override public void onTick(Minecraft mc) {
        if (mc.player.tickCount % 60 == 0) scan(mc);
    }

    private static boolean isActivity(BlockState s) {
        Block b = s.getBlock();
        return b instanceof ChestBlock || b instanceof BarrelBlock || b instanceof AbstractFurnaceBlock
                || b instanceof ShulkerBoxBlock || b instanceof HopperBlock || b instanceof BedBlock
                || b instanceof CraftingTableBlock || b instanceof EnderChestBlock || b instanceof DispenserBlock;
    }

    private static boolean isGeode(BlockState s) {
        return s.is(Blocks.BUDDING_AMETHYST) || s.is(Blocks.AMETHYST_BLOCK);
    }

    private void scan(Minecraft mc) {
        List<Hit> res = new ArrayList<>();
        int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
        int r = simDistance.asInt();
        int threshold = Math.max(1, (int) Math.round(12.0 / sensitivity.value));
        Predicate<BlockState> interesting = s -> isActivity(s) || isGeode(s);

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                LevelChunk ch = mc.level.getChunkSource().getChunkNow(pcx + dx, pcz + dz);
                if (ch == null) continue;
                int bud = 0, ame = 0, act = 0;
                double geoY = 0, actY = 0;
                LevelChunkSection[] secs = ch.getSections();
                for (int i = 0; i < secs.length; i++) {
                    LevelChunkSection sec = secs[i];
                    if (sec.hasOnlyAir() || !sec.maybeHas(interesting)) continue;
                    int baseY = (ch.getMinSectionY() + i) << 4;
                    for (int y = 0; y < 16; y++)
                        for (int z = 0; z < 16; z++)
                            for (int x = 0; x < 16; x++) {
                                BlockState st = sec.getBlockState(x, y, z);
                                if (st.is(Blocks.BUDDING_AMETHYST)) { bud++; geoY += baseY + y; }
                                else if (st.is(Blocks.AMETHYST_BLOCK)) { ame++; geoY += baseY + y; }
                                else if (isActivity(st)) { act++; actY += baseY + y; }
                            }
                }
                int cx = pcx + dx, cz = pcz + dz;
                if (geodes.value && (bud >= 1 || ame >= 4))
                    res.add(new Hit(cx, cz, "Geode", geoY / (bud + ame)));
                else if (activity.value && act >= threshold)
                    res.add(new Hit(cx, cz, "Activity (" + act + ")", actY / act));
            }
        }
        hits = res;
    }

    @Override public void onHud(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        int line = color.argb(alpha.asInt()), text = 0xFF000000 | color.value;
        for (Hit h : hits) {
            double x0 = h.cx * 16.0, z0 = h.cz * 16.0, y = h.y;
            Vec3 a = new Vec3(x0, y, z0), b = new Vec3(x0 + 16, y, z0),
                    c = new Vec3(x0 + 16, y, z0 + 16), d = new Vec3(x0, y, z0 + 16);
            Draw.line3D(g, a, b, line); Draw.line3D(g, b, c, line);
            Draw.line3D(g, c, d, line); Draw.line3D(g, d, a, line);
            double[] p = Draw.project(new Vec3(x0 + 8, y + 1, z0 + 8));
            if (p != null) g.drawCenteredString(mc.font, h.label, (int) p[0], (int) p[1], text);
        }
    }
}
