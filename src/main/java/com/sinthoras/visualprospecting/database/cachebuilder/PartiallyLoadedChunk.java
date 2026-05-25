package com.sinthoras.visualprospecting.database.cachebuilder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import com.gtnewhorizon.gtnhlib.util.data.ImmutableBlockMeta;
import com.sinthoras.visualprospecting.VP;

import bartworks.system.material.Werkstoff;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.IOreMaterial;
import gregtech.common.ores.BWOreAdapter;
import gregtech.common.ores.GTOreAdapter;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;
import it.unimi.dsi.fastutil.ints.Int2ShortFunction;

public class PartiallyLoadedChunk {

    private static final int SECTION_SIZE = 16;
    private static final int BLOCKS_PER_EBS = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;
    public static final int CHUNK_HEIGHT = 256;
    private static final int SECTIONS_PER_CHUNK = CHUNK_HEIGHT / SECTION_SIZE;

    private final Int2ShortFunction[] blocks = new Int2ShortFunction[SECTIONS_PER_CHUNK];
    private final Int2ShortFunction[] metas = new Int2ShortFunction[SECTIONS_PER_CHUNK];

    private List<NBTTagCompound> tiles;

    @SuppressWarnings("unchecked")
    public void load(NBTTagCompound chunk, int chunkX, int chunkZ) {
        NBTTagList sections = chunk.getCompoundTag("Level").getTagList("Sections", NBT.TAG_COMPOUND);

        List<NBTTagCompound> sectionTags = (List<NBTTagCompound>) sections.tagList;

        for (NBTTagCompound section : sectionTags) {
            byte y = section.getByte("Y");

            if (section.hasKey("Blocks16")) {
                // chunk saved with NotEnoughIds

                ShortBuffer blocks = ByteBuffer.wrap(section.getByteArray("Blocks16")).asShortBuffer();
                ShortBuffer metas = ByteBuffer.wrap(section.getByteArray("Data16")).asShortBuffer();

                if (blocks.capacity() == 0 || metas.capacity() == 0) {
                    // no-op for empty sections
                    this.blocks[y] = i -> (short) 0;
                    this.metas[y] = i -> (short) 0;
                    continue;
                }

                if (blocks.capacity() != BLOCKS_PER_EBS) {
                    VP.LOG.error(
                            "Corrupt section detected at X={}, Y={}, Z={} (Blocks16 length was {}, needs {})",
                            chunkX,
                            y,
                            chunkZ,
                            blocks.capacity(),
                            BLOCKS_PER_EBS);
                    continue;
                }

                if (metas.capacity() != BLOCKS_PER_EBS) {
                    VP.LOG.error(
                            "Corrupt section detected at X={}, Y={}, Z={} (Data16 length was {}, needs {})",
                            chunkX,
                            y,
                            chunkZ,
                            metas.capacity(),
                            BLOCKS_PER_EBS);
                    continue;
                }

                this.blocks[y] = blocks::get;
                this.metas[y] = metas::get;

            } else if (section.hasKey("Add") || section.hasKey("BlocksB2Hi")) {
                // chunk saved with EndlessIds

                // 00FF
                ByteBuffer blocks1Byte = ByteBuffer.wrap(section.getByteArray("Blocks"));
                // 0F00
                ByteBuffer blocks2Lo;
                if (section.hasKey("Add")) {
                    blocks2Lo = ByteBuffer.wrap(section.getByteArray("Add"));
                } else {
                    blocks2Lo = null;
                }
                // F000
                ByteBuffer blocks2Hi;
                if (section.hasKey("BlocksB2Hi")) {
                    blocks2Hi = ByteBuffer.wrap(section.getByteArray("BlocksB2Hi"));
                } else {
                    blocks2Hi = null;
                }

                this.blocks[y] = i -> {
                    int nibbleShift = (i & 1) * 4;
                    short id = (short) (blocks1Byte.get(i) & 0xFF);
                    if (blocks2Lo != null) {
                        id |= (short) (((blocks2Lo.get(i >> 1) >> nibbleShift) & 0xF) << 8);
                    }
                    if (blocks2Hi != null) {
                        id |= (short) (((blocks2Hi.get(i >> 1) >> nibbleShift) & 0xF) << 12);
                    }
                    return id;
                };

                // 000F
                ByteBuffer meta1Lo = ByteBuffer.wrap(section.getByteArray("Data"));
                // 00F0
                ByteBuffer meta1Hi;
                if (section.hasKey("Data1High")) {
                    meta1Hi = ByteBuffer.wrap(section.getByteArray("Data1High"));
                } else {
                    meta1Hi = null;
                }
                // FF00
                ByteBuffer meta2;
                if (section.hasKey("Data2")) {
                    meta2 = ByteBuffer.wrap(section.getByteArray("Data2"));
                } else {
                    meta2 = null;
                }

                this.metas[y] = i -> {
                    int nibbleShift = (i & 1) * 4;
                    short meta = 0;
                    meta |= (short) (((meta1Lo.get(i >> 1) >> nibbleShift) & 0xF));
                    if (meta1Hi != null) {
                        meta |= (short) (((meta1Hi.get(i >> 1) >> nibbleShift) & 0xF) << 4);
                    }
                    if (meta2 != null) {
                        meta |= (short) ((meta2.get(i) & 0xFF) << 8);
                    }
                    return meta;
                };

            } else {
                // neither NotEnoughIds nor EndlessIds
                // don't bother loading the actual blocks, if a chunk is this old it'll only have ore tiles
                this.blocks[y] = i -> (short) 0;
                this.metas[y] = i -> (short) 0;
            }
        }

        tiles = chunk.getCompoundTag("Level").getTagList("TileEntities", NBT.TAG_COMPOUND).tagList;
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

    public interface OreConsumer {

        void visit(int x, int y, int z, OreInfo<IOreMaterial> ore);
    }

    @SuppressWarnings("unchecked")
    public void forEachOre(OreConsumer consumer) {
        for (PartiallyLoadedChunk.TileEntityInfo te : tileEntities()) {
            if (!te.getId().equals("GT_TileEntity_Ores")) continue;

            short meta = te.getTag().getShort("m");
            boolean natural = te.getTag().getBoolean("n");

            if (!natural) continue;

            ImmutableBlockMeta bm = GTOreAdapter.INSTANCE.transform(meta, true);

            try (OreInfo<Materials> info = GTOreAdapter.INSTANCE.getOreInfo(bm.getBlock(), bm.getBlockMeta())) {
                if (info == null || info.isSmall || info.material == null) continue;

                consumer.visit(te.getX(), te.getY(), te.getZ(), (OreInfo<IOreMaterial>) (OreInfo<?>) info);
            }
        }

        for (PartiallyLoadedChunk.TileEntityInfo te : tileEntities()) {
            if (!te.getId().equals("bw.blockoresTE")) continue;

            short meta = te.getTag().getShort("m");
            boolean natural = te.getTag().getBoolean("n");

            if (!natural) continue;

            ImmutableBlockMeta bm = BWOreAdapter.INSTANCE.transform(meta, true, false);

            try (OreInfo<Werkstoff> info = BWOreAdapter.INSTANCE.getOreInfo(bm.getBlock(), bm.getBlockMeta())) {
                if (info == null || info.isSmall || info.material == null) continue;

                consumer.visit(te.getX(), te.getY(), te.getZ(), (OreInfo<IOreMaterial>) (OreInfo<?>) info);
            }
        }

        for (int y = 0; y < PartiallyLoadedChunk.CHUNK_HEIGHT; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    Block block = getBlock(x, y, z);
                    int meta = getBlockMeta(x, y, z);

                    try (OreInfo<IOreMaterial> info = OreManager.getOreInfo(block, meta)) {
                        if (info == null || info.isSmall || !info.isNatural || info.material == null) continue;

                        consumer.visit(x, y, z, info);
                    }
                }
            }
        }
    }

    public interface TileEntityInfo {

        String getId();

        int getX();

        int getY();

        int getZ();

        NBTTagCompound getTag();
    }

    public Iterable<TileEntityInfo> tileEntities() {
        return TileEntityIterator::new;
    }

    private class TileEntityIterator implements Iterator<TileEntityInfo> {

        private final TileEntityInfoImpl info = new TileEntityInfoImpl();

        private int tileIndex;

        @Override
        public boolean hasNext() {
            return tileIndex < tiles.size();
        }

        @Override
        public TileEntityInfo next() {
            info.tag = tiles.get(tileIndex++);

            return info;
        }
    }

    private static class TileEntityInfoImpl implements TileEntityInfo {

        public NBTTagCompound tag;

        @Override
        public String getId() {
            return tag.getString("id");
        }

        @Override
        public int getX() {
            return tag.getInteger("x");
        }

        @Override
        public int getY() {
            return tag.getInteger("y");
        }

        @Override
        public int getZ() {
            return tag.getInteger("z");
        }

        @Override
        public NBTTagCompound getTag() {
            return tag;
        }
    }
}
