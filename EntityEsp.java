package com.faro.client.module;

import com.faro.client.render.Draw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class EntityEsp extends Module {
    public final Setting.Bool players = add(new Setting.Bool("Players", true));
    public final Setting.Bool mobs = add(new Setting.Bool("Mobs & animals", true));
    public final Setting.Bool others = add(new Setting.Bool("Other entities", false));
    public final Setting.Bool names = add(new Setting.Bool("Show names", true));
    public final Setting.Bool gear = add(new Setting.Bool("Show armor & held items", true));
    public final Setting.Bool playerTracers = add(new Setting.Bool("Tracers: players", false));
    public final Setting.Bool entityTracers = add(new Setting.Bool("Tracers: other entities", false));
    public final Setting.Color playerColor = add(new Setting.Color("Player color", 0xC447FF));
    public final Setting.Color mobColor = add(new Setting.Color("Entity color", 0xFFB84D));
    public final Setting.Num range = add(new Setting.Num("Max distance", 128, 16, 256, 8));

    public EntityEsp() {
        super("ESP", Category.ESP, "See players & entities through walls");
    }

    @Override public void onHud(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        boolean detached = Freecam.INSTANCE.isEnabled() || Freelook.INSTANCE.isEnabled();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player && !detached) continue;
            if (e.isRemoved() || e.distanceTo(mc.player) > range.value) continue;
            boolean isPlayer = e instanceof Player;
            if (isPlayer ? !players.value : (e instanceof LivingEntity ? !mobs.value : !others.value)) continue;

            int color = 0xFF000000 | (isPlayer ? playerColor.value : mobColor.value);
            AABB bb = e.getBoundingBox().move(Mth.lerp(pt, e.xo, e.getX()) - e.getX(),
                    Mth.lerp(pt, e.yo, e.getY()) - e.getY(), Mth.lerp(pt, e.zo, e.getZ()) - e.getZ());

            if (isPlayer ? playerTracers.value : entityTracers.value) Draw.tracer(g, bb.getCenter(), color);
            int[] r = Draw.box(g, bb, color);
            if (r == null) continue;

            int cx = (r[0] + r[2]) / 2, y = r[1] - 10;
            if (names.value) g.drawCenteredString(mc.font, e.getName().getString(), cx, y, color);
            if (gear.value && e instanceof LivingEntity le) {
                List<ItemStack> items = new ArrayList<>();
                for (ItemStack s : new ItemStack[]{le.getMainHandItem(), le.getOffhandItem(),
                        le.getItemBySlot(EquipmentSlot.HEAD), le.getItemBySlot(EquipmentSlot.CHEST),
                        le.getItemBySlot(EquipmentSlot.LEGS), le.getItemBySlot(EquipmentSlot.FEET)})
                    if (!s.isEmpty()) items.add(s);
                int x = cx - items.size() * 17 / 2;
                for (ItemStack s : items) { g.renderItem(s, x, y - 18); x += 17; }
            }
        }
    }
}
