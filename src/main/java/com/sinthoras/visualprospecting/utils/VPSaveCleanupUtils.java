package com.sinthoras.visualprospecting.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sinthoras.visualprospecting.database.WorldIdHandler;

public final class VPSaveCleanupUtils {

    /**
     * Normally you obtain the player UUID by doing {@code Minecraft.getMinecraft().thePlayer.getPersistentID()}
     * <p>
     * This is not possible in the game state where a save deletion occurs because it's not loaded yet. Therefore, the
     * only option to find the player UUID is to attempt to read it from cache in usernamecache.json.
     * </p>
     * 
     * @param mcDataDir       Full path to Minecraft instance
     * @param currentUsername Name of current user
     * @return The UUID matching the current user logged in the client or null if not found
     */
    public static UUID getUUIDForCurrentUser(File mcDataDir, String currentUsername) {

        File usernameFile = new File(mcDataDir, "usernamecache.json");
        if (!usernameFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(usernameFile)) {
            JsonObject usernameObj = new JsonParser().parse(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : usernameObj.entrySet()) {
                JsonElement value = entry.getValue();
                if (value != null && currentUsername.equalsIgnoreCase(value.getAsString())) {
                    try {
                        return UUID.fromString(entry.getKey());
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * In case {@link #getUUIDForCurrentUser(File, String)} fail at helping us locate the data this allows us to attempt
     * an alternative approach.
     * 
     * @return UUID for an offline player
     */
    public static UUID getUUIDFromBytesForCurrentUser(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(UTF_8));
    }

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
