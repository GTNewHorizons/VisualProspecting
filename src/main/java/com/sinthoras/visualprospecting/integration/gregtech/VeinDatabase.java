package com.sinthoras.visualprospecting.integration.gregtech;

import java.util.Optional;

import net.minecraft.world.ChunkCoordIntPair;

import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;

import gregtech.crossmod.visualprospecting.IDatabase;

public class VeinDatabase implements IDatabase {

    @Override
    public Optional<String> getVeinName(int dimensionId, ChunkCoordIntPair coordinates) {
        OreVeinPosition oreVein = ServerCache.instance
                .getOreVein(dimensionId, coordinates.chunkXPos, coordinates.chunkZPos);
        if (oreVein != null && oreVein.veinType != VeinType.NO_VEIN) {
            // Unfortunately, there's not a very good way to localize this. This method is used to drive information
            // in the Metrics Transmitter cover in GT5U, which operates entirely server-side. At best, we could try to
            // capture which language the original cover attaching player is using, maybe. (I think this requires
            // reflection, of course. Why should things be easy?) Even then, it wouldn't help in a multiplayer scenario
            // with users having different locales.
            return Optional.of(oreVein.veinType.getVeinName());
        } else {
            return Optional.empty();
        }
    }
}
