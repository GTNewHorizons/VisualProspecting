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

    private final RandomAccessFile is;

    public RegionReader(File regionFile) throws IOException, DataFormatException {
        is = new RandomAccessFile(regionFile, "r");
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
        is.seek(l * 4L);
        int location = (is.read() << 16) + (is.read() << 8) + is.read();
        int size = is.read();
        if (size > 0) {
            // Chunk non-void, load it
            is.seek(location * 4096L);
            // Read 4-bytes of data length
            int compressedLength = (is.read() << 24) | (is.read() << 16) | (is.read() << 8) | is.read();
            // Read compression mode
            int compression = is.read();
            if (compression != 2) return null;

            byte[] compressedDataBuffer = new byte[compressedLength];
            is.readFully(compressedDataBuffer, 0, compressedLength);
            try (DataInputStream chunkData = new DataInputStream(
                    new BufferedInputStream(
                            new InflaterInputStream(
                                    new ByteArrayInputStream(compressedDataBuffer, 0, compressedLength))))) {
                return CompressedStreamTools.read(chunkData);
            }
        }
        return null;
    }

    public void close() throws IOException {
        is.close();
    }
}
