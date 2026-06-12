package com.sinthoras.visualprospecting.database.cachebuilder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
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

public class PartiallyLoadedChunk {

    private static final int SECTION_SIZE = 16;
    private static final int BLOCKS_PER_EBS = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;
    public static final int CHUNK_HEIGHT = 256;
    private static final int SECTIONS_PER_CHUNK = CHUNK_HEIGHT / SECTION_SIZE;
    private static final int NIBBLE_ARRAY_SIZE = BLOCKS_PER_EBS / 2; // Expected byte-array lengths for nibble arrays

    private final Int2IntFunction[] blocks = new Int2IntFunction[SECTIONS_PER_CHUNK];
    private final Int2IntFunction[] metas = new Int2IntFunction[SECTIONS_PER_CHUNK];

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

            } else if (section.hasKey("Add") || section.hasKey("BlocksB2Hi") || section.hasKey("Data1High")) {
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
                // "Add", "BlocksB2Hi", "BlocksB3", "Data1High", and "Data2" are all optional
                // Reference:
                // https://github.com/GTMEGA/EndlessIDs/blob/master/src/main/java/com/falsepattern/endlessids/managers/BlockIDManager.java
                // https://github.com/GTMEGA/EndlessIDs/blob/master/src/main/java/com/falsepattern/endlessids/managers/BlockMetaManager.java
                loadEndlessIds(section, chunkX, y, chunkZ);

            } else {
                // Vanilla format or unrecognized: skip block data.
                // Old chunks only have ore TileEntities.
                this.blocks[y] = i -> 0;
                this.metas[y] = i -> 0;
            }
        }

        tiles = chunk.getCompoundTag("Level").getTagList("TileEntities", NBT.TAG_COMPOUND).tagList;
    }

    /**
     * Populates this chunk from an in-memory Chunk, to allow for real time debug command.
     */
    public void loadFromLiveChunk(Chunk chunk) {
        final ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        for (int section = 0; section < SECTIONS_PER_CHUNK; section++) {
            final ExtendedBlockStorage ebs = section < storage.length ? storage[section] : null;
            if (ebs == null) {
                this.blocks[section] = i -> 0;
                this.metas[section] = i -> 0;
                continue;
            }
            // withinSection index layout (matches forEachOre): bits 0-3 = x, 4-7 = z, 8-11 = y
            this.blocks[section] = i -> Block
                    .getIdFromBlock(ebs.getBlockByExtId(i & 0xF, (i >> 8) & 0xF, (i >> 4) & 0xF));
            this.metas[section] = i -> ebs.getExtBlockMetadata(i & 0xF, (i >> 8) & 0xF, (i >> 4) & 0xF);
        }

        this.tiles = new ArrayList<>();
        for (TileEntity tileEntity : chunk.chunkTileEntityMap.values()) {
            final NBTTagCompound tag = new NBTTagCompound();
            tileEntity.writeToNBT(tag);
            this.tiles.add(tag);
        }
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
            this.metas[y] = i -> 0;
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

        // Mask with 0xFFFF to avoid sign extension when widening short -> int (IDs/metas >= 0x8000).
        this.blocks[y] = i -> blocks.get(i) & 0xFFFF;
        this.metas[y] = i -> metas.get(i) & 0xFFFF;
    }

    /**
     * Loads a chunk section saved in EndlessIDs format. "Blocks" and "Data" are mandatory; all other arrays are
     * optional and treated as all-zero when absent. A corrupt mandatory array zeroes the whole section.
     */
    private void loadEndlessIds(NBTTagCompound section, int chunkX, byte y, int chunkZ) {
        ByteBuffer blockB1 = loadOptionalArray(section, "Blocks", BLOCKS_PER_EBS, chunkX, y, chunkZ);
        ByteBuffer metaM1Lo = loadOptionalArray(section, "Data", NIBBLE_ARRAY_SIZE, chunkX, y, chunkZ);
        if (blockB1 == null || metaM1Lo == null) {
            VP.LOG.error(
                    "Corrupt EndlessIDs section at X={}, Y={}, Z={}: missing or invalid mandatory Blocks/Data array",
                    chunkX,
                    y,
                    chunkZ);
            this.blocks[y] = i -> 0;
            this.metas[y] = i -> 0;
            return;
        }

        ByteBuffer blockB2Lo = loadOptionalArray(section, "Add", NIBBLE_ARRAY_SIZE, chunkX, y, chunkZ);
        ByteBuffer blockB2Hi = loadOptionalArray(section, "BlocksB2Hi", NIBBLE_ARRAY_SIZE, chunkX, y, chunkZ);
        ByteBuffer blockB3 = loadOptionalArray(section, "BlocksB3", BLOCKS_PER_EBS, chunkX, y, chunkZ);

        this.blocks[y] = i -> {
            int nibbleShift = (i & 1) * 4;
            int id = blockB1.get(i) & 0xFF;
            if (blockB2Lo != null) id |= ((blockB2Lo.get(i >> 1) >> nibbleShift) & 0xF) << 8;
            if (blockB2Hi != null) id |= ((blockB2Hi.get(i >> 1) >> nibbleShift) & 0xF) << 12;
            if (blockB3 != null) id |= (blockB3.get(i) & 0xFF) << 16;
            return id;
        };

        ByteBuffer metaM1Hi = loadOptionalArray(section, "Data1High", NIBBLE_ARRAY_SIZE, chunkX, y, chunkZ);
        ByteBuffer metaM2 = loadOptionalArray(section, "Data2", BLOCKS_PER_EBS, chunkX, y, chunkZ);

        this.metas[y] = i -> {
            int nibbleShift = (i & 1) * 4;
            int meta = (metaM1Lo.get(i >> 1) >> nibbleShift) & 0xF;
            if (metaM1Hi != null) meta |= ((metaM1Hi.get(i >> 1) >> nibbleShift) & 0xF) << 4;
            if (metaM2 != null) meta |= (metaM2.get(i) & 0xFF) << 8;
            return meta;
        };
    }

    /**
     * Reads an optional byte array from a section NBT compound. Validates the length against {@code expectedLength} and
     * returns null if the key is absent or the array is malformed.
     */
    private ByteBuffer loadOptionalArray(NBTTagCompound section, String key, int expectedLength, int chunkX, byte y,
            int chunkZ) {
        if (!section.hasKey(key)) return null;
        byte[] data = section.getByteArray(key);
        if (data.length != expectedLength) {
            VP.LOG.error(
                    "Corrupt EndlessIDs section at X={}, Y={}, Z={}: {} length {} (expected {})",
                    chunkX,
                    y,
                    chunkZ,
                    key,
                    data.length,
                    expectedLength);
            return null;
        }
        return ByteBuffer.wrap(data);
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

        // Iterate per-section so the blocks/metas array lookups are done out of the inner loop
        for (int section = 0; section < SECTIONS_PER_CHUNK; section++) {
            Int2IntFunction blocks = this.blocks[section];
            Int2IntFunction metas = this.metas[section];

            int baseY = section << 4;
            for (int dy = 0; dy < SECTION_SIZE; dy++) {
                int y = baseY + dy;
                for (int z = 0; z < SECTION_SIZE; z++) {
                    for (int x = 0; x < SECTION_SIZE; x++) {
                        int withinSection = (dy << 8) | (z << 4) | x;
                        int id = blocks == null ? 0 : blocks.get(withinSection);
                        int meta = metas == null ? 0 : metas.get(withinSection);
                        Block block = Block.getBlockById(id);

                        try (OreInfo<IOreMaterial> info = OreManager.getOreInfo(block, meta)) {
                            if (info == null || info.isSmall || !info.isNatural || info.material == null) continue;

                            consumer.visit(x, y, z, info);
                        }
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
