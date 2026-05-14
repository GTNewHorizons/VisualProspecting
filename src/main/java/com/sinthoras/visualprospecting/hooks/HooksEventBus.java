package com.sinthoras.visualprospecting.hooks;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.WorldEvent;

import org.apache.commons.io.FileUtils;

import com.gtnewhorizon.gtnhlib.client.event.WorldDeletionEvent;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.utils.VPSaveCleanupUtils;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class HooksEventBus {

    @SubscribeEvent
    public void onEvent(WorldEvent.Unload event) {
        if (Utils.isLogicalClient()) {
            ClientCache.instance.saveVeinCache();
        }
    }

    @SubscribeEvent
    public void onEvent(WorldEvent.Save event) {
        ServerCache.instance.saveVeinCache();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onWorldDeletionEvent(WorldDeletionEvent event) {
        File mcDataDir = Minecraft.getMinecraft().mcDataDir;
        Path vpBasePath = mcDataDir.toPath().resolve(Tags.MODID);
        String currentUsername = Minecraft.getMinecraft().getSession().getUsername();

        String worldId = VPSaveCleanupUtils.getVisualProspectingWorldId(mcDataDir, event.worldName);
        if (worldId == null) {
            VP.LOG.warn("Unable to read VP worldID for world \"{}\", can't cleanup any VP data", event.worldName);
            return;
        }
        Path vpServerPathFull = vpBasePath.resolve("server").resolve(worldId);
        if (Files.isDirectory(vpServerPathFull)) {
            try {
                FileUtils.deleteDirectory(vpServerPathFull.toFile());
            } catch (IOException e) {
                VP.LOG.warn("Failed to delete Visual Prospecting server data found at {}", vpServerPathFull);
            }
        }

        UUID userUUID = Minecraft.getMinecraft().getSession().func_148256_e().getId();
        if (userUUID == null) {
            VP.LOG.debug("Unable to read userUUID for current user. Attempting to use fallback instead");
            userUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + currentUsername).getBytes(UTF_8));
        }
        Path vpClientPathFull = vpBasePath.resolve("client").resolve(userUUID.toString()).resolve(worldId);
        if (Files.isDirectory(vpClientPathFull)) {
            try {
                FileUtils.deleteDirectory(vpClientPathFull.toFile());
            } catch (IOException e) {
                VP.LOG.warn("Failed to delete Visual Prospecting client data found at {}", vpClientPathFull);
            }
        }

    }
}
