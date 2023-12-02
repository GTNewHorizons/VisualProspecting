package com.sinthoras.visualprospecting.utils;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;

import io.netty.buffer.ByteBuf;

public final class VPByteBufUtils {

    public static void WriteOreVeinPosition(ByteBuf buf, OreVeinPosition oreVeinPosition) {
        buf.writeInt(oreVeinPosition.dimensionId);
        buf.writeInt(oreVeinPosition.chunkX);
        buf.writeInt(oreVeinPosition.chunkZ);
        buf.writeShort(oreVeinPosition.veinType.veinId);
        buf.writeBoolean(oreVeinPosition.isDepleted());
    }

    public static OreVeinPosition ReadOreVeinPosition(ByteBuf buf) {
        final int dimId = buf.readInt();
        final int chunkX = buf.readInt();
        final int chunkZ = buf.readInt();
        final short veinId = buf.readShort();
        final boolean isDepleted = buf.readBoolean();

        return new OreVeinPosition(dimId, chunkX, chunkZ, VeinTypeCaching.getVeinType(veinId), isDepleted);
    }

    public static void WriteUndergroundFluidPosition(ByteBuf buf, UndergroundFluidPosition undergroundFluidPosition) {
        buf.writeInt(undergroundFluidPosition.dimensionId);
        buf.writeInt(undergroundFluidPosition.chunkX);
        buf.writeInt(undergroundFluidPosition.chunkZ);
        buf.writeInt(undergroundFluidPosition.fluid.getID());
        for (int offsetChunkX = 0; offsetChunkX < VP.undergroundFluidSizeChunkX; offsetChunkX++) {
            for (int offsetChunkZ = 0; offsetChunkZ < VP.undergroundFluidSizeChunkZ; offsetChunkZ++) {
                buf.writeInt(undergroundFluidPosition.chunks[offsetChunkX][offsetChunkZ]);
            }
        }
    }

    public static UndergroundFluidPosition ReadUndergroundFluidPosition(ByteBuf buf) {
        final int dimId = buf.readInt();
        final int chunkX = buf.readInt();
        final int chunkZ = buf.readInt();
        final Fluid fluid = FluidRegistry.getFluid(buf.readInt());
        final int[][] chunks = new int[VP.undergroundFluidSizeChunkX][VP.undergroundFluidSizeChunkZ];
        for (int offsetChunkX = 0; offsetChunkX < VP.undergroundFluidSizeChunkX; offsetChunkX++) {
            for (int offsetChunkZ = 0; offsetChunkZ < VP.undergroundFluidSizeChunkZ; offsetChunkZ++) {
                chunks[offsetChunkX][offsetChunkZ] = buf.readInt();
            }
        }

        return new UndergroundFluidPosition(dimId, chunkX, chunkZ, fluid, chunks);
    }

    public static void WriteOreVeinPositions(ByteBuf buf, List<OreVeinPosition> oreVeinPositions) {
        buf.writeInt(oreVeinPositions.size());
        for (OreVeinPosition oreVeinPosition : oreVeinPositions) {
            WriteOreVeinPosition(buf, oreVeinPosition);
        }
    }

    public static List<OreVeinPosition> ReadOreVeinPositions(ByteBuf buf) {
        List<OreVeinPosition> oreVeinPositions = new ArrayList<>();

        final int oreVeinCount = buf.readInt();
        for (int i = 0; i < oreVeinCount; i++) {
            oreVeinPositions.add(ReadOreVeinPosition(buf));
        }

        return oreVeinPositions;
    }

    public static void WriteUndergroundFluidPositions(ByteBuf buf,
            List<UndergroundFluidPosition> undergroundFluidPositions) {
        buf.writeInt(undergroundFluidPositions.size());
        for (UndergroundFluidPosition undergroundFluidPosition : undergroundFluidPositions) {
            WriteUndergroundFluidPosition(buf, undergroundFluidPosition);
        }
    }

    public static List<UndergroundFluidPosition> ReadUndergroundFluidPositions(ByteBuf buf) {
        List<UndergroundFluidPosition> undergroundFluidPositions = new ArrayList<>();

        final int undergroundFluidCount = buf.readInt();
        for (int i = 0; i < undergroundFluidCount; i++) {
            undergroundFluidPositions.add(ReadUndergroundFluidPosition(buf));
        }

        return undergroundFluidPositions;
    }
}
