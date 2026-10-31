package com.faro.client.module;

import com.faro.client.render.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class NameTags extends Module {
    public final Setting.Bool icon = add(new Setting.Bool("Show item icon", true));
    public final Setting.Bool count = add(new Setting.Bool("Show stack count", true));
    public final Setting.Num range = add(new Setting.Num("Max distance", 64, 8, 128, 4));

    public NameTags() {
        super("NameTags", Category.ESP, "Name + icon above dropped items");
    }

    @Override public void onHud(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ItemEntity ie) || ie.distanceTo(mc.player) > range.value) continue;
            ItemStack st = ie.getItem();
            Vec3 top = new Vec3(Mth.lerp(pt, e.xo, e.getX()),
                    Mth.lerp(pt, e.yo, e.getY()) + e.getBbHeight() + 0.35, Mth.lerp(pt, e.zo, e.getZ()));
            double[] p = Draw.project(top);
            if (p == null) continue;
            String txt = st.getHoverName().getString() + (count.value && st.getCount() > 1 ? " x" + st.getCount() : "");
            int total = (icon.value ? 18 : 0) + mc.font.width(txt);
            int x = (int) p[0] - total / 2, y = (int) p[1] - 8;
            g.fill(x - 3, y - 1, x + total + 3, y + 17, 0x99000000);
            if (icon.value) { g.renderItem(st, x, y); x += 18; }
            g.drawString(mc.font, txt, x, y + 4, 0xFFFFFFFF, false);
        }
    }
}
