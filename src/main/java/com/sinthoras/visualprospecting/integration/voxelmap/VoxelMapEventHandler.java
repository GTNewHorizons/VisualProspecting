package com.sinthoras.visualprospecting.integration.voxelmap;

import java.util.TreeSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.navigator.api.voxelmap.VoxelMapWaypointManager;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.hooks.ProspectingNotificationEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class VoxelMapEventHandler {

    @SubscribeEvent
    public void onVeinProspected(ProspectingNotificationEvent.OreVein event) {
        if (event.isCanceled()) {
            return;
        }

        OreVeinPosition pos = event.getPosition();
        short[] color = pos.veinType.primaryOre.getRGBA();
        TreeSet<Integer> dim = new TreeSet<>();
        dim.add(pos.dimensionId);
        VoxelMapWaypointManager.addVoxelMapWaypoint(
                pos.veinType.getVeinName(), // name
                pos.getBlockX(), // X
                pos.getBlockZ(), // Z
                getY(), // Y
                Config.enableVoxelMapWaypointsByDefault, // enabled
                (float) color[0] / 255.0f, // red
                (float) color[1] / 255.0f, // green
                (float) color[2] / 255.0f, // blue
                "Pickaxe", // icon
                dim);
    }

    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public void onFluidProspected(ProspectingNotificationEvent.UndergroundFluid event) {
        if (event.isCanceled()) {
            return;
        }

        UndergroundFluidPosition pos = event.getPosition();
        int x = Utils.coordChunkToBlock(pos.chunkX);
        int z = Utils.coordChunkToBlock(pos.chunkZ);
        int color = pos.fluid.getColor();
        TreeSet<Integer> dim = new TreeSet<>();
        dim.add(pos.dimensionId);

        VoxelMapWaypointManager.addVoxelMapWaypoint(
                pos.fluid.getLocalizedName(), // name
                x, // X
                z, // Z
                Minecraft.getMinecraft().theWorld.getHeightValue(x, z), // Y
                Config.enableVoxelMapWaypointsByDefault, // enabled
                (float) (color >> 16 & 0xFF) / 255.0f, // red
                (float) (color >> 8 & 0xFF) / 255.0f, // green
                (float) (color & 0xFF) / 255.0f, // blue
                "Science", // icon
                dim); // dimension
    }

    private static int getY() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || !heldItem.getUnlocalizedName().contains("gt.detrav.metatool.01")) {
            return (int) player.posY;
        }
        return 65;
    }
}
