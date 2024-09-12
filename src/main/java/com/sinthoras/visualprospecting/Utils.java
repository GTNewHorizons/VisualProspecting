package com.sinthoras.visualprospecting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.sinthoras.visualprospecting.hooks.HooksClient;

import cpw.mods.fml.common.Loader;
import gregtech.common.GTWorldgenerator;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Utils {

    public static boolean isNEIInstalled() {
        return Loader.isModLoaded("NotEnoughItems");
    }

    public static boolean isNavigatorInstalled() {
        return Loader.isModLoaded("navigator");
    }

    public static int coordBlockToChunk(int blockCoord) {
        return blockCoord < 0 ? -((-blockCoord - 1) >> 4) - 1 : blockCoord >> 4;
    }

    public static int coordChunkToBlock(int chunkCoord) {
        return chunkCoord < 0 ? -((-chunkCoord) << 4) : chunkCoord << 4;
    }

    public static long chunkCoordsToKey(int chunkX, int chunkZ) {
        return CoordinatePacker.pack(chunkX, 0, chunkZ);
    }

    public static int mapToCenterOreChunkCoord(final int chunkCoord) {
        if (GTWorldgenerator.oregenPattern == GTWorldgenerator.OregenPattern.EQUAL_SPACING) {
            // new evenly spaced ore pattern
            return chunkCoord - Math.floorMod(chunkCoord, 3) + 1;
        } else {
            // old bugged ore pattern
            if (chunkCoord >= 0) {
                return chunkCoord - (chunkCoord % 3) + 1;
            } else {
                return chunkCoord - (chunkCoord % 3) - 1;
            }
        }
    }

    public static int mapToCornerUndergroundFluidChunkCoord(final int chunkCoord) {
        return chunkCoord & 0xFFFFFFF8;
    }

    public static boolean isSmallOreId(short metaData) {
        return metaData >= VP.gregTechSmallOreMinimumMeta;
    }

    public static short oreIdToMaterialId(short metaData) {
        return (short) (metaData % 1000);
    }

    public static boolean isLogicalClient() {
        return VPMod.proxy instanceof HooksClient;
    }

    public static File getMinecraftDirectory() {
        if (isLogicalClient()) {
            return Minecraft.getMinecraft().mcDataDir;
        } else {
            return new File(".");
        }
    }

    public static File getSubDirectory(final String subdirectory) {
        return new File(getMinecraftDirectory(), subdirectory);
    }

    public static void deleteDirectoryRecursively(final File targetDirectory) {
        try {
            try (Stream<Path> files = Files.walk(targetDirectory.toPath())) {
                files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Short> readFileToMap(File file) {
        if (!file.exists()) {
            return new HashMap<>();
        }
        try {
            final Gson gson = new Gson();
            final Reader reader = Files.newBufferedReader(file.toPath());
            final Map<String, Short> map = gson.fromJson(reader, new TypeToken<Map<String, Short>>() {}.getType());
            reader.close();
            return map;
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public static void writeMapToFile(File file, Map<String, Short> map) {
        try {
            if (file.exists()) {
                file.delete();
            }
            final Gson gson = new Gson();
            final Writer writer = Files.newBufferedWriter(file.toPath(), StandardOpenOption.CREATE_NEW);
            gson.toJson(map, new TypeToken<Map<String, Short>>() {}.getType(), writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeNBT(File file, NBTTagCompound tag) {
        try (FileOutputStream stream = new FileOutputStream(newFile(file))) {
            CompressedStreamTools.writeCompressed(tag, stream);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static File newFile(File file) {
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    @Nullable
    public static NBTTagCompound readNBT(@NotNull File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        try (InputStream stream = new FileInputStream(file)) {
            return CompressedStreamTools.readCompressed(stream);
        } catch (Exception ex) {
            try {
                return CompressedStreamTools.read(file);
            } catch (Exception ex1) {
                return null;
            }
        }
    }

    public static Map<Integer, ByteBuffer> getLegacyDimFiles(File directory) {
        if (!directory.exists()) {
            return new HashMap<>();
        }

        try {
            final List<Integer> dimensionIds = Files.walk(directory.toPath(), 1).filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("DIM"))
                    .map(dimensionFolder -> Integer.parseInt(dimensionFolder.getFileName().toString().substring(3)))
                    .collect(Collectors.toList());
            final Map<Integer, ByteBuffer> dimensionFiles = new HashMap<>();
            for (int dimensionId : dimensionIds) {
                ByteBuffer buffer = readFileToBuffer(new File(directory.toPath() + "/DIM" + dimensionId));
                if (buffer != null) {
                    dimensionFiles.put(dimensionId, buffer);
                }
            }
            return dimensionFiles;

        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private static ByteBuffer readFileToBuffer(File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            final FileInputStream inputStream = new FileInputStream(file);
            final FileChannel inputChannel = inputStream.getChannel();
            final ByteBuffer buffer = ByteBuffer.allocate((int) inputChannel.size());

            inputChannel.read(buffer);
            buffer.flip();

            inputChannel.close();
            inputStream.close();

            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
