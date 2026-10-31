package com.faro.client.module;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public abstract class Setting<T> {
    public final String name;
    public T value;

    protected Setting(String name, T value) { this.name = name; this.value = value; }

    public static class Bool extends Setting<Boolean> {
        public Bool(String n, boolean v) { super(n, v); }
    }

    public static class Num extends Setting<Double> {
        public final double min, max, step;
        public Num(String n, double def, double min, double max, double step) {
            super(n, def); this.min = min; this.max = max; this.step = step;
        }
        public void set(double v) {
            v = Math.max(min, Math.min(max, v));
            if (step > 0) v = Math.round(v / step) * step;
            value = v;
        }
        public void setFraction(double f) { set(min + (max - min) * Math.max(0, Math.min(1, f))); }
        public double fraction() { return (value - min) / (max - min); }
        public double get() { return value; }
        public int asInt() { return (int) Math.round(value); }
        public String display() {
            return step >= 1 ? String.valueOf(asInt()) : String.format(Locale.ROOT, "%.1f", value);
        }
    }

    public static class Color extends Setting<Integer> {
        public static final int[] PALETTE = {0xC447FF, 0xFF4D4D, 0xFFB84D, 0xFFE94D, 0x4DFF88, 0x4DD2FF, 0x4D6BFF, 0xFFFFFF};
        public Color(String n, int rgb) { super(n, rgb); }
        public void cycle(int dir) {
            int i = 0;
            for (int k = 0; k < PALETTE.length; k++) if (PALETTE[k] == value) i = k;
            value = PALETTE[Math.floorMod(i + dir, PALETTE.length)];
        }
        public int argb(int alpha) { return (alpha << 24) | value; }
        public String hex() { return String.format("#%06X", value); }
    }

    public static class Text extends Setting<String> {
        public Text(String n, String v) { super(n, v); }
    }

    /** Komma-getrennte Blockliste, z.B. "diamond_ore,ancient_debris" */
    public static class BlockList extends Text {
        private static Map<String, Block> LOOKUP;
        private String cacheKey = null;
        private Set<Block> cache = Set.of();

        public BlockList(String n, String v) { super(n, v); }

        private static Map<String, Block> lookup() {
            if (LOOKUP == null) {
                LOOKUP = new HashMap<>();
                for (Block b : BuiltInRegistries.BLOCK) {
                    String id = BuiltInRegistries.BLOCK.getKey(b).toString();
                    LOOKUP.put(id, b);
                    LOOKUP.put(id.substring(id.indexOf(':') + 1), b);
                }
            }
            return LOOKUP;
        }

        public Set<Block> blocks() {
            if (!value.equals(cacheKey)) {
                cacheKey = value;
                Set<Block> s = new HashSet<>();
                for (String part : value.split(",")) {
                    String k = part.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
                    Block b = lookup().get(k);
                    if (b != null) s.add(b);
                }
                cache = s;
            }
            return cache;
        }
    }
}
