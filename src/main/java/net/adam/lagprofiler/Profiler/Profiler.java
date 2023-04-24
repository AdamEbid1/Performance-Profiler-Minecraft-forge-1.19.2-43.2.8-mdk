package net.adam.lagprofiler.Profiler;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Profiler {

    public void profileChunks(Level world) {
        if (!(world instanceof ServerLevel)) return;

        ServerLevel serverWorld = (ServerLevel) world;
        Map<ChunkPos, Long> chunkDurations = new HashMap<>();

        LongSet forcedChunks = serverWorld.getForcedChunks();
        forcedChunks.forEach(packedPos -> {
            int x = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(SectionPos.x(packedPos)));
            int z = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(SectionPos.z(packedPos)));
            ChunkPos pos = new ChunkPos(x, z);

            LevelChunk chunk = serverWorld.getChunk(x, z);

            long startTime = System.nanoTime();
            chunk.getBlockEntities().values().forEach(blockEntity -> {
                Block block = blockEntity.getBlockState().getBlock();
                if (block instanceof EntityBlock) {
                    BlockEntityTicker ticker = ((EntityBlock) block).getTicker(world, blockEntity.getBlockState(), blockEntity.getType());
                    if (ticker != null) {
                        ticker.tick(world, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity);
                    }
                }
            });
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            chunkDurations.put(pos, duration);
        });

        printTopLaggyChunks(chunkDurations, 5);
    }
    private void printTopLaggyChunks(Map<ChunkPos, Long> chunkDurations, int topCount) {
        System.out.println("Top " + topCount + " laggiest forced chunks:");
        chunkDurations.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(topCount)
                .forEach(entry -> {
                    ChunkPos pos = entry.getKey();
                    long duration = entry.getValue();
                    System.out.println("Chunk (" + pos.x + ", " + pos.z + ") - Duration: " + duration + " ns");
                });
    }
}