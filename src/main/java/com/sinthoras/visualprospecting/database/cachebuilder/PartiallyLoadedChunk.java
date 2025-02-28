package com.sinthoras.visualprospecting.database.cachebuilder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import com.sinthoras.visualprospecting.VP;

import it.unimi.dsi.fastutil.ints.Int2ShortFunction;

public class PartiallyLoadedChunk {

    private static final int SECTION_SIZE = 16;
    private static final int BLOCKS_PER_EBS = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;
    public static final int CHUNK_HEIGHT = 256;
    private static final int SECTIONS_PER_CHUNK = CHUNK_HEIGHT / SECTION_SIZE;

    private final Int2ShortFunction[] blocks = new Int2ShortFunction[SECTIONS_PER_CHUNK];
    private final Int2ShortFunction[] metas = new Int2ShortFunction[SECTIONS_PER_CHUNK];

    @SuppressWarnings("unchecked")
    public void load(NBTTagCompound chunk, int chunkX, int chunkZ) {
        NBTTagList sections = chunk.getCompoundTag("Level").getTagList("Sections", NBT.TAG_COMPOUND);

        List<NBTTagCompound> sectionTags = (List<NBTTagCompound>) sections.tagList;

        for (NBTTagCompound section : sectionTags) {
            byte y = section.getByte("Y");

            ShortBuffer blocks = ByteBuffer.wrap(section.getByteArray("Blocks16")).asShortBuffer();
            ShortBuffer metas = ByteBuffer.wrap(section.getByteArray("Data16")).asShortBuffer();

            if (blocks.capacity() != BLOCKS_PER_EBS || metas.capacity() != BLOCKS_PER_EBS) {
                VP.LOG.error("Corrupt chunk detected at X={}, Z={}", chunkX, chunkZ);
                continue;
            }

            this.blocks[y] = blocks::get;
            this.metas[y] = metas::get;
        }
    }

    public int getBlockId(int x, int y, int z) {
        if (x < 0 || x >= 16) throw new IllegalArgumentException("x");
        if (y < 0 || y >= CHUNK_HEIGHT) throw new IllegalArgumentException("y");
        if (z < 0 || z >= 16) throw new IllegalArgumentException("z");

        int index = y << 8 | z << 4 | x;

        int section = index / BLOCKS_PER_EBS;

        Int2ShortFunction blocks = this.blocks[section];

        if (blocks == null) return 0;

        int withinSection = index % BLOCKS_PER_EBS;

        return blocks.get(withinSection);
    }

    public Block getBlock(int x, int y, int z) {
        return Block.getBlockById(getBlockId(x, y, z));
    }

    public int getBlockMeta(int x, int y, int z) {
        if (x < 0 || x >= 16) throw new IllegalArgumentException("x");
        if (y < 0 || y >= CHUNK_HEIGHT) throw new IllegalArgumentException("y");
        if (z < 0 || z >= 16) throw new IllegalArgumentException("z");

        int index = y << 8 | z << 4 | x;

        int section = index / BLOCKS_PER_EBS;

        Int2ShortFunction metas = this.metas[section];

        if (metas == null) return 0;

        int withinSection = index % BLOCKS_PER_EBS;

        return metas.get(withinSection);
    }
}
