package com.sinthoras.visualprospecting.integration.model.render;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;

import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidLocation;

import journeymap.api.v2.client.display.ImageOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.common.Context;
import journeymap.api.v2.common.util.BlockPos;

public final class UndergroundFluidImageOverlay {

    private static final int CELL_PIXELS = 8;

    private UndergroundFluidImageOverlay() {}

    public static Collection<ImageOverlay> create(UndergroundFluidLocation location) {
        BufferedImage image = new BufferedImage(
                VP.undergroundFluidSizeChunkX * CELL_PIXELS,
                VP.undergroundFluidSizeChunkZ * CELL_PIXELS,
                BufferedImage.TYPE_INT_ARGB);
        if (location.isActive()) {
            int min = location.getMinProduction();
            int max = location.getMaxProduction();
            float range = max - min + 1;
            for (int x = 0; x < VP.undergroundFluidSizeChunkX; x++) {
                for (int z = 0; z < VP.undergroundFluidSizeChunkZ; z++) {
                    int amount = location.getChunks()[x][z];
                    if (amount <= 0) continue;
                    int alpha = range > 1 ? (int) ((amount - min) / range * 255) : 10;
                    fillCell(image, x, z, alpha << 24 | location.getColor());
                }
            }
        }
        drawBorder(
                image,
                (location.getMaxProduction() > 0 ? location.getColor() : 0xFFFFFF)
                        | (location.isActive() && location.getMaxProduction() > 0 ? 0xFF : 74) << 24);

        int x = (int) Math.floor(location.getBlockX());
        int z = (int) Math.floor(location.getBlockZ());
        int width = VP.undergroundFluidSizeChunkX * VP.chunkWidth;
        int height = VP.undergroundFluidSizeChunkZ * VP.chunkWidth;
        MapImage mapImage = new MapImage(image).setBlur(false);
        ImageOverlay overlay = new ImageOverlay(
                Tags.MODID,
                new BlockPos(x, 64, z),
                new BlockPos(x + width, 64, z + height),
                mapImage);
        overlay.setDimension(location.getDimensionId()).setOverlayGroupName(UndergroundFluidLocation.class.getName())
                .setActiveUIs(Context.UI.Minimap);
        return Collections.singletonList(overlay);
    }

    private static void fillCell(BufferedImage image, int cellX, int cellZ, int argb) {
        for (int x = cellX * CELL_PIXELS; x < (cellX + 1) * CELL_PIXELS; x++) {
            for (int z = cellZ * CELL_PIXELS; z < (cellZ + 1) * CELL_PIXELS; z++) {
                image.setRGB(x, z, argb);
            }
        }
    }

    private static void drawBorder(BufferedImage image, int argb) {
        for (int x = 0; x < image.getWidth(); x++) {
            image.setRGB(x, 0, argb);
            image.setRGB(x, image.getHeight() - 1, argb);
        }
        for (int z = 0; z < image.getHeight(); z++) {
            image.setRGB(0, z, argb);
            image.setRGB(image.getWidth() - 1, z, argb);
        }
    }
}
