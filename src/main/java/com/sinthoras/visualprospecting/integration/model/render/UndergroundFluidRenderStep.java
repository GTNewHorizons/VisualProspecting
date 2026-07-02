package com.sinthoras.visualprospecting.integration.model.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import com.gtnewhorizons.navigator.api.model.steps.UniversalRenderStep;
import com.gtnewhorizons.navigator.api.util.DrawUtils;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.integration.model.layers.UndergroundFluidLayerManager;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidLocation;

import gregtech.common.UndergroundOil;

public class UndergroundFluidRenderStep extends UniversalRenderStep<UndergroundFluidLocation> {

    public UndergroundFluidRenderStep(UndergroundFluidLocation location) {
        super(location);
        setSize(VP.undergroundFluidSizeChunkX * VP.chunkWidth);
        setFontScale(0.5f);
    }

    @Override
    public void draw(double topX, double topY, float drawScale, double zoom) {
        final Minecraft mc = Minecraft.getMinecraft();
        final double regionW = VP.undergroundFluidSizeChunkX * VP.chunkWidth * blockSize;
        final double regionH = VP.undergroundFluidSizeChunkZ * VP.chunkWidth * blockSize;
        if (topX + regionW < 0 || topY + regionH < 0 || topX > mc.displayWidth || topY > mc.displayHeight) {
            return;
        }

        renderChunks(topX, topY);
        setSize(VP.undergroundFluidSizeChunkX * VP.chunkWidth);
        final int maxAmountInField = location.getMaxProduction();
        if (maxAmountInField > 0) {
            int alpha = location.isActive() ? 255 : 74;
            DrawUtils
                    .drawHollowRect(topX, topY, getAdjustedWidth(), getAdjustedHeight(), location.getColor(), alpha, 2);
        } else {
            DrawUtils.drawHollowRect(topX, topY, getAdjustedWidth(), getAdjustedHeight(), 0xFFFFFF, 74, 2);
        }

        if (!isMinimap()) {
            String label = I18n.format("visualprospecting.empty");
            if (maxAmountInField > 0) {
                label = getFluidAmountFormatted(location.getMinProduction()) + " - "
                        + getFluidAmountFormatted(maxAmountInField)
                        + "  "
                        + location.getFluid().getLocalizedName();
            }

            int textColor = 0xFFFFFFFF;
            if (UndergroundFluidLayerManager.instance.isSearchActive()) {
                textColor = location.isActive() ? 0xFFFF00 : 0x444444;
            }

            DrawUtils.drawLabel(
                    label,
                    topX + getAdjustedWidth() / 2,
                    topY + 1.5,
                    textColor,
                    0xB4000000,
                    true,
                    getFontScale());
        }
    }

    private void renderChunks(double x, double y) {
        final double zoomStep = getZoomStep();
        if (zoomStep < Config.minZoomLevelForUndergroundFluidDetails - 1 || location.getMaxProduction() <= 0
                || !location.isActive()) {
            return;
        }
        final boolean drawLabels = zoomStep >= Config.minZoomLevelForUndergroundFluidDetails;

        final int minProduction = location.getMinProduction();
        final int maxProduction = location.getMaxProduction();
        final int fluidColor = location.getColor();
        final int[][] chunks = location.getChunks();
        final float productionRange = maxProduction - minProduction + 1;
        final boolean highlightPeak = maxProduction >= 10;

        final Minecraft mc = Minecraft.getMinecraft();
        final double screenW = mc.displayWidth;
        final double screenH = mc.displayHeight;
        setSize(VP.chunkWidth);
        final double cellW = getAdjustedWidth();
        final double cellH = getAdjustedHeight();
        for (int chunkX = 0; chunkX < VP.undergroundFluidSizeChunkX; chunkX++) {
            final double cellX = x + chunkX * cellW;
            if (cellX + cellW < 0 || cellX > screenW) continue;
            for (int chunkZ = 0; chunkZ < VP.undergroundFluidSizeChunkZ; chunkZ++) {
                final double cellY = y + chunkZ * cellH;
                if (cellY + cellH < 0 || cellY > screenH) continue;
                int amount = chunks[chunkX][chunkZ];
                if (amount <= 0) continue;
                int alpha = productionRange > 1 ? (int) ((amount - minProduction) / productionRange * 255) : 10;
                DrawUtils.drawRect(cellX, cellY, cellW, cellH, fluidColor, alpha);

                if (highlightPeak && amount >= maxProduction) {
                    DrawUtils.drawHollowRect(cellX, cellY, cellW, cellH, 0xFFD700, 204, 1.5);
                }

                if (drawLabels) {
                    DrawUtils.drawLabel(
                            getFluidAmountFormatted(amount),
                            cellX + cellW / 2,
                            cellY + cellH / 2,
                            0xFFFFFFFF,
                            0xB4000000,
                            true,
                            getFontScale());
                }
            }
        }
    }

    private String getFluidAmountFormatted(int amount) {
        return getLitresFormatted((long) amount * UndergroundOil.DIVIDER);
    }

    private String getLitresFormatted(long litres) {
        if (litres >= 1_000_000_000) {
            return getRoundedAmountFormatted(litres, 1_000_000_000, "GL");
        }
        if (litres >= 1_000_000) {
            return getRoundedAmountFormatted(litres, 1_000_000, "ML");
        }
        if (litres >= 1_000) {
            return getRoundedAmountFormatted(litres, 1_000, "kL");
        }
        return litres + " L";
    }

    private String getRoundedAmountFormatted(long litres, long scale, String unit) {
        final long tenths = (litres * 10 + scale / 2) / scale;
        final long whole = tenths / 10;
        final long decimal = tenths % 10;
        if (decimal == 0) {
            return whole + " " + unit;
        }
        return whole + "." + decimal + " " + unit;
    }
}
