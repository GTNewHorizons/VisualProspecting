package com.sinthoras.visualprospecting.database;

import com.sinthoras.visualprospecting.VPTags;
import com.sinthoras.visualprospecting.VPUtils;
import com.sinthoras.visualprospecting.database.veintypes.VPVeinType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VPServerOreCache extends VPWorldOreCache {

    protected File getStorageDirectory() {
        return VPUtils.getSubDirectory(VPTags.SERVER_DIR);
    }

    public boolean putVeinType(int dimensionId, int chunkX, int chunkZ, final VPVeinType veinType) {
        return super.putVeinType(dimensionId, chunkX, chunkZ, veinType);
    }

    public List<VPProspectionResult> prospectChunks(int dimensionId, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        minChunkX = VPUtils.mapToCenterOreChunkCoord(minChunkX);
        minChunkZ = VPUtils.mapToCenterOreChunkCoord(minChunkZ);
        maxChunkX = VPUtils.mapToCenterOreChunkCoord(maxChunkX);
        maxChunkZ = VPUtils.mapToCenterOreChunkCoord(maxChunkZ);

        List<VPProspectionResult> prospectionResult = new ArrayList<>();
        for(int chunkX = minChunkX;chunkX <= maxChunkX;chunkX+=3)
            for(int chunkZ = minChunkZ;chunkZ <= maxChunkZ;chunkZ+=3) {
                final VPVeinType veinType = getVeinType(dimensionId, chunkX, chunkZ);
                if(veinType != VPVeinType.NO_VEIN) {
                    prospectionResult.add(new VPProspectionResult(chunkX, chunkZ, veinType));
                }
            }
        return prospectionResult;
    }

    public List<VPProspectionResult> prospectBlocks(int dimensionId, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        return prospectChunks(dimensionId,
                VPUtils.coordBlockToChunk(minBlockX),
                VPUtils.coordBlockToChunk(minBlockZ),
                VPUtils.coordBlockToChunk(maxBlockX),
                VPUtils.coordBlockToChunk(maxBlockZ));
    }

    public List<VPProspectionResult> prospectBlockRadius(int dimensionId, int blockX, int blockZ, int blockRadius) {
        return prospectBlocks(dimensionId, blockX - blockRadius, blockZ - blockRadius, blockX + blockRadius, blockZ + blockRadius);
    }
}
