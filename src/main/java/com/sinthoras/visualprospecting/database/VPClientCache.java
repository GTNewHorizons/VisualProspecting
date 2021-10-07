package com.sinthoras.visualprospecting.database;

import com.sinthoras.visualprospecting.*;
import com.sinthoras.visualprospecting.database.veintypes.VPVeinType;
import com.sinthoras.visualprospecting.network.VPProspectingRequest;
import gregtech.common.blocks.GT_TileEntity_Ores;
import journeymap.client.JourneymapClient;
import journeymap.client.data.WorldData;
import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.io.FileHandler;
import journeymap.common.Journeymap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.io.File;

public class VPClientCache extends VPWorldCache{

    public boolean loadVeinCache(String worldId) {
        final Minecraft minecraft = ForgeHelper.INSTANCE.getClient();

        if (worldId != null) {
            worldId = worldId.replaceAll("\\W+", "~");
        }

        final String suffix = minecraft.isSingleplayer() ? "" : worldId != null ? "_" + worldId : "";
        final File gamemodeFilder = new File(FileHandler.MinecraftDirectory, minecraft.isSingleplayer() ? VPTags.CLIENT_SP_DIR : VPTags.CLIENT_MP_DIR);
        final File worldCacheDirectory = new File(gamemodeFilder, WorldData.getWorldName(minecraft, false) + suffix);
        return super.loadVeinCache(worldCacheDirectory);
    }

    protected void onNewVein(VPVeinType veinType) {
        Minecraft.getMinecraft().thePlayer.sendChatMessage(veinType.name);
    }

    public void onOreInteracted(World world, int blockX, int blockY, int blockZ, EntityPlayer entityPlayer) {
        if(VPConfig.enableProspecting
                && ((JourneymapClient) Journeymap.proxy).isMapping()
                && Minecraft.getMinecraft().thePlayer == entityPlayer) {
            final TileEntity tTileEntity = world.getTileEntity(blockX, blockY, blockZ);
            if (tTileEntity instanceof GT_TileEntity_Ores) {
                final short oreMetaData = ((GT_TileEntity_Ores) tTileEntity).mMetaData;
                if (oreMetaData < VP.gregTechSmallOreMinimumMeta
                        && oreMetaData != 0) {
                    final int chunkX = VPUtils.coordBlockToChunk(blockX);
                    final int chunkZ = VPUtils.coordBlockToChunk(blockZ);
                    final VPVeinType veinType = getVeinType(entityPlayer.dimension, chunkX, chunkZ);
                    if(veinType.containsOre((short)(oreMetaData % 1000)) == false
                            && VPProspectingRequest.canSendRequest()) {
                        VP.network.sendToServer(new VPProspectingRequest(entityPlayer.dimension, blockX, blockY, blockZ, (short)(oreMetaData % 1000)));
                    }
                }
            }
        }
    }
}