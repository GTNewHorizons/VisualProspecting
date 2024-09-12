package com.sinthoras.visualprospecting.database.cachebuilder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.DataFormatException;
import java.util.zip.InflaterInputStream;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class RegionReader implements AutoCloseable {

    RandomAccessFile is;
    private final int[] locations = new int[1024];
    private final int[] sizes = new int[1024];

    public RegionReader(File regionFile) throws IOException, DataFormatException {
        is = new RandomAccessFile(regionFile, "r");
        // First read the 1024 chunks offsets
        for (int i = 0; i < 1024; i++) {
            locations[i] += is.read() << 16;
            locations[i] += is.read() << 8;
            locations[i] += is.read();

            sizes[i] += is.read();
        }
        // Discard the timestamp bytes, we don't care.
        byte[] osef = new byte[4];
        for (int i = 0; i < 1024; i++) {
            is.read(osef);
        }
    }

    private int offset(int x, int z) {
        return ((x & 31) + (z & 31) * 32);
    }

    public @Nullable NBTTagCompound getChunk(int x, int z) throws DataFormatException, IOException {
        return getChunkInternal(x, z);
    }

    public @Nullable NBTTagList getChunkTiles(int x, int z) {
        try {
            NBTTagCompound chunk = getChunkInternal(x, z);
            if (chunk == null) return null;

            NBTTagCompound level = chunk.getCompoundTag("Level");

            if (level.hasKey("TileEntities")) {
                return level.getTagList("TileEntities", 10);
            }
            return null;
        } catch (DataFormatException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private @Nullable NBTTagCompound getChunkInternal(int x, int z) throws DataFormatException, IOException {
        int l = offset(x, z);
        if (sizes[l] > 0) {
            // Chunk non-void, load it
            is.seek(locations[l] * 4096L);
            // Read 4-bytes of data length
            int compressedLength = 0;
            compressedLength += is.read() << 24;
            compressedLength += is.read() << 16;
            compressedLength += is.read() << 8;
            compressedLength += is.read();
            // Read compression mode
            int compression = is.read();
            if (compression != 2) {
                throw new DataFormatException(
                        "\"Fatal error : compression scheme not Zlib. (\" + compression + \") at \" + is.getFilePointer() + \" l = \" + l + \" s= \" + sizes[l]");
            }
            byte[] compressedData = new byte[compressedLength];
            is.read(compressedData);
            DataInputStream chunkData = new DataInputStream(
                    new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(compressedData))));
            NBTTagCompound tag = CompressedStreamTools.read(chunkData);
            chunkData.close();
            return tag;
        }
        return null;
    }

    public void close() throws IOException {
        is.close();
    }
}
