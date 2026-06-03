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
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.Int2ShortFunction;

public class PartiallyLoadedChunk {

    private static final int SECTION_SIZE = 16;
    private static final int BLOCKS_PER_EBS = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;
    public static final int CHUNK_HEIGHT = 256;
    private static final int SECTIONS_PER_CHUNK = CHUNK_HEIGHT / SECTION_SIZE;
    private static final int NIBBLE_ARRAY_SIZE = BLOCKS_PER_EBS / 2; // Expected byte-array lengths for nibble arrays

    private final Int2IntFunction[] blocks = new Int2IntFunction[SECTIONS_PER_CHUNK];
    private final Int2ShortFunction[] metas = new Int2ShortFunction[SECTIONS_PER_CHUNK];

    private List<NBTTagCompound> tiles;

    @SuppressWarnings("unchecked")
    public void load(NBTTagCompound chunk, int chunkX, int chunkZ) {
        NBTTagList sections = chunk.getCompoundTag("Level").getTagList("Sections", NBT.TAG_COMPOUND);

        List<NBTTagCompound> sectionTags = (List<NBTTagCompound>) sections.tagList;

        for (NBTTagCompound section : sectionTags) {
            byte y = section.getByte("Y");

            if (section.hasKey("Blocks16")) {
                // NotEnoughIDs format: full 16-bit block IDs and metas stored as ShortBuffers.
                loadNotEnoughIds(section, chunkX, y, chunkZ);

            } else if (section.hasKey("Add") || section.hasKey("BlocksB2Hi")) {
                // EndlessIDs format: block ID and meta split across multiple nibble/byte arrays.
                //
                // Block ID layout (24 bits total):
                // bits 0x0000FF — "Blocks" byte array (vanilla low 8 bits)
                // bits 0x000F00 — "Add" nibble array (NotEnoughIDs extension to 12 bits)
                // bits 0x00F000 — "BlocksB2Hi" nibble array (EndlessIDs extension to 16 bits)
                // bits 0xFF0000 — "BlocksB3" byte array (EndlessIDs extension to 24 bits)
                //
                // Meta layout (16 bits total):
                // bits 0x000F — "Data" nibble array (vanilla low 4 bits)
                // bits 0x00F0 — "Data1High" nibble array (EndlessIDs extension)
                // bits 0xFF00 — "Data2" byte array (EndlessIDs extension, 1 byte per block)
                //
                // "Add", "BlocksB2Hi", "BlocksB3", "Data1High", and "Data2" are all optional —
                // absent arrays mean those bits are zero, preserving backwards compatibility.
                // Reference:
                // https://github.com/GTMEGA/EndlessIDs/blob/master/src/main/java/com/falsepattern/endlessids/managers/BlockIDManager.java
                // https://github.com/GTMEGA/EndlessIDs/blob/master/src/main/java/com/falsepattern/endlessids/managers/BlockMetaManager.java
                loadEndlessIds(section, chunkX, y, chunkZ);

            } else {
                // Vanilla format or unrecognized: skip block data.
                // Old chunks only have ore TileEntities.
                this.blocks[y] = i -> 0;
                this.metas[y] = i -> (short) 0;
            }
        }

        tiles = chunk.getCompoundTag("Level").getTagList("TileEntities", NBT.TAG_COMPOUND).tagList;
    }

    /**
     * Loads a chunk section saved in NotEnoughIDs format ("Blocks16" / "Data16"). Block IDs and metas are stored as
     * flat ShortBuffers - one short per block, no bit packing.
     */
    private void loadNotEnoughIds(NBTTagCompound section, int chunkX, byte y, int chunkZ) {
        ShortBuffer blocks = ByteBuffer.wrap(section.getByteArray("Blocks16")).asShortBuffer();
        ShortBuffer metas = ByteBuffer.wrap(section.getByteArray("Data16")).asShortBuffer();

        if (blocks.capacity() == 0 || metas.capacity() == 0) {
            this.blocks[y] = i -> 0;
            this.metas[y] = i -> (short) 0;
            return;
        }

        if (blocks.capacity() != BLOCKS_PER_EBS) {
            VP.LOG.error(
                    "Corrupt NotEnoughIDs section at X={}, Y={}, Z={}: Blocks16 length {} (expected {})",
                    chunkX,
                    y,
                    chunkZ,
                    blocks.capacity(),
                    BLOCKS_PER_EBS);
            return;
        }

        if (metas.capacity() != BLOCKS_PER_EBS) {
            VP.LOG.error(
                    "Corrupt NotEnoughIDs section at X={}, Y={}, Z={}: Data16 length {} (expected {})",
                    chunkX,
                    y,
                    chunkZ,
                    metas.capacity(),
                    BLOCKS_PER_EBS);
            return;
        }

        this.blocks[y] = blocks::get;
        this.metas[y] = metas::get;
    }

    /**
     * Loads a chunk section saved in EndlessIDs format. Missing optional arrays are treated as all-zero.
     */
    private void loadEndlessIds(NBTTagCompound section, int chunkX, byte y, int chunkZ) {
        // --- Block ID arrays ---
        byte[] blocksRaw = section.getByteArray("Blocks");
        if (blocksRaw.length != BLOCKS_PER_EBS) {
            VP.LOG.error(
                    "Corrupt EndlessIDs section at X={}, Y={}, Z={}: Blocks length {} (expected {})",
                    chunkX,
                    y,
                    chunkZ,
                    blocksRaw.length,
                    BLOCKS_PER_EBS);
            this.blocks[y] = i -> 0;
            this.metas[y] = i -> (short) 0;
            return;
        }
        ByteBuffer blockLo = ByteBuffer.wrap(blocksRaw);

        ByteBuffer blockMid = loadNibbleArray(section, "Add", chunkX, y, chunkZ);
        ByteBuffer blockHi = loadNibbleArray(section, "BlocksB2Hi", chunkX, y, chunkZ);

        // BlocksB3: bits 16-23, one byte per block, extends IDs to 24 bits
        ByteBuffer blockB3 = null;
        if (section.hasKey("BlocksB3")) {
            byte[] b3 = section.getByteArray("BlocksB3");
            if (b3.length != BLOCKS_PER_EBS) {
                VP.LOG.error(
                        "Corrupt EndlessIDs section at X={}, Y={}, Z={}: BlocksB3 length {} (expected {})",
                        chunkX,
                        y,
                        chunkZ,
                        b3.length,
                        BLOCKS_PER_EBS);
            } else {
                blockB3 = ByteBuffer.wrap(b3);
            }
        }
        final ByteBuffer fBlockB3 = blockB3;

        this.blocks[y] = i -> {
            int nibbleShift = (i & 1) * 4;
            int id = blockLo.get(i) & 0xFF;
            if (blockMid != null) id |= ((blockMid.get(i >> 1) >> nibbleShift) & 0xF) << 8;
            if (blockHi != null) id |= ((blockHi.get(i >> 1) >> nibbleShift) & 0xF) << 12;
            if (fBlockB3 != null) id |= (fBlockB3.get(i) & 0xFF) << 16;
            return id;
        };

        // --- Meta arrays ---
        ByteBuffer metaLo = loadNibbleArray(section, "Data", chunkX, y, chunkZ);
        ByteBuffer metaMid = loadNibbleArray(section, "Data1High", chunkX, y, chunkZ);

        ByteBuffer metaHi = null;
        if (section.hasKey("Data2")) {
            byte[] data2 = section.getByteArray("Data2");
            if (data2.length != BLOCKS_PER_EBS) {
                VP.LOG.error(
                        "Corrupt EndlessIDs section at X={}, Y={}, Z={}: Data2 length {} (expected {})",
                        chunkX,
                        y,
                        chunkZ,
                        data2.length,
                        BLOCKS_PER_EBS);
            } else {
                metaHi = ByteBuffer.wrap(data2);
            }
        }

        final ByteBuffer fMetaLo = metaLo;
        final ByteBuffer fMetaMid = metaMid;
        final ByteBuffer fMetaHi = metaHi;

        this.metas[y] = i -> {
            int nibbleShift = (i & 1) * 4;
            short meta = 0;
            if (fMetaLo != null) meta |= (short) ((fMetaLo.get(i >> 1) >> nibbleShift) & 0xF);
            if (fMetaMid != null) meta |= (short) (((fMetaMid.get(i >> 1) >> nibbleShift) & 0xF) << 4);
            if (fMetaHi != null) meta |= (short) ((fMetaHi.get(i) & 0xFF) << 8);
            return meta;
        };
    }

    /**
     * Reads an optional nibble array from a section NBT compound. Validates the length ({@link #NIBBLE_ARRAY_SIZE}) and
     * returns null if the key is absent or the array is malformed, so callers can treat null as all-zero bits.
     */
    private ByteBuffer loadNibbleArray(NBTTagCompound section, String key, int chunkX, byte y, int chunkZ) {
        if (!section.hasKey(key)) return null;
        byte[] data = section.getByteArray(key);
        if (data.length != NIBBLE_ARRAY_SIZE) {
            VP.LOG.error(
                    "Corrupt EndlessIDs section at X={}, Y={}, Z={}: {} nibble array length {} (expected {})",
                    chunkX,
                    y,
                    chunkZ,
                    key,
                    data.length,
                    NIBBLE_ARRAY_SIZE);
            return null;
        }
        return ByteBuffer.wrap(data);
    }

    public int getBlockId(int x, int y, int z) {
        if (x < 0 || x >= 16) throw new IllegalArgumentException("x");
        if (y < 0 || y >= CHUNK_HEIGHT) throw new IllegalArgumentException("y");
        if (z < 0 || z >= 16) throw new IllegalArgumentException("z");

        int index = y << 8 | z << 4 | x;

        int section = index / BLOCKS_PER_EBS;

        Int2IntFunction blocks = this.blocks[section];

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
