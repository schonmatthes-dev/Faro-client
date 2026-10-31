package com.faro.client.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

/** Sucht bestimmte Blocks in den geladenen Chunks um den Spieler. */
public final class BlockScanner {
    public static List<BlockPos> scan(Set<Block> blocks, int radius, int limit) {
        List<BlockPos> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (blocks.isEmpty() || level == null || mc.player == null) return out;

        int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
        int cr = (radius >> 4) + 1;
        double r2 = (double) radius * radius;
        Vec3 pp = mc.player.position();
        Predicate<BlockState> pred = s -> blocks.contains(s.getBlock());

        for (int dx = -cr; dx <= cr; dx++) {
            for (int dz = -cr; dz <= cr; dz++) {
                int cx = pcx + dx, cz = pcz + dz;
                LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
                if (ch == null) continue;
                LevelChunkSection[] secs = ch.getSections();
                for (int i = 0; i < secs.length; i++) {
                    LevelChunkSection sec = secs[i];
                    if (sec.hasOnlyAir() || !sec.maybeHas(pred)) continue;
                    int baseY = (ch.getMinSectionY() + i) << 4;
                    for (int y = 0; y < 16; y++)
                        for (int z = 0; z < 16; z++)
                            for (int x = 0; x < 16; x++) {
                                if (!blocks.contains(sec.getBlockState(x, y, z).getBlock())) continue;
                                int wx = (cx << 4) + x, wy = baseY + y, wz = (cz << 4) + z;
                                if (pp.distanceToSqr(wx + .5, wy + .5, wz + .5) > r2) continue;
                                out.add(new BlockPos(wx, wy, wz));
                                if (out.size() >= limit) return out;
                            }
                }
            }
        }
        return out;
    }
}
