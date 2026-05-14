package com.sinthoras.visualprospecting.utils;

import java.io.File;
import java.io.FileInputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import com.sinthoras.visualprospecting.database.WorldIdHandler;

public final class VPSaveCleanupUtils {

    /**
     * The name of save folder is created in {@link WorldIdHandler} by combining world name and a randomly generated
     * UUID. The value is then saved into visualprospecting.dat file so we have to go read it there.
     * 
     * @param mcDataDir   Full path to Minecraft instance
     * @param worldFolder Directory name for world we want the VP worldId of
     * @return worldId VP value for this world
     */
    public static String getVisualProspectingWorldId(File mcDataDir, String worldFolder) {
        File vpDat = mcDataDir.toPath().resolve("saves").resolve(worldFolder).resolve("data")
                .resolve("visualprospecting.dat").toFile();
        if (!vpDat.exists()) {
            return null;
        }

        try (FileInputStream vpFIS = new FileInputStream(vpDat)) {
            NBTTagCompound vpNBT = CompressedStreamTools.readCompressed(vpFIS);
            if (vpNBT.hasKey("data")) {
                NBTTagCompound data = vpNBT.getCompoundTag("data");
                if (data.hasKey("wId")) {
                    return data.getString("wId");
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
