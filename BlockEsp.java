package com.faro.client.module;

import com.faro.client.render.Draw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class BlockEsp extends Module {
    public final Setting.BlockList blocks = add(new Setting.BlockList("Blocks (comma separated)",
            "diamond_ore,deepslate_diamond_ore,ancient_debris"));
    public final Setting.Color color = add(new Setting.Color("Color", 0x4DD2FF));
    public final Setting.Bool tracers = add(new Setting.Bool("Tracers", false));
    public final Setting.Num radius = add(new Setting.Num("Radius", 48, 8, 128, 8));
    private List<BlockPos> found = new ArrayList<>();

    public BlockEsp() {
        super("Block ESP", Category.ESP, "Highlight chosen blocks through walls");
    }

    @Override protected void onEnable() { found = new ArrayList<>(); }

    @Override public void onTick(Minecraft mc) {
        if (mc.player.tickCount % 20 == 0) found = BlockScanner.scan(blocks.blocks(), radius.asInt(), 400);
    }

    @Override public void onHud(GuiGraphics g, float pt) {
        int c = 0xFF000000 | color.value;
        for (BlockPos p : found) {
            if (tracers.value) Draw.tracer(g, p.getCenter(), c);
            Draw.box(g, new AABB(p), c);
        }
    }
}
