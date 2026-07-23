package com.sinthoras.visualprospecting.integration.model.render;

import java.text.MessageFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import com.gtnewhorizons.navigator.api.model.steps.UniversalRenderStep;
import com.gtnewhorizons.navigator.api.util.DrawUtils;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.integration.model.layers.UndergroundFluidLayerManager;
import com.sinthoras.visualprospecting.integration.model.locations.UndergroundFluidLocation;

public class UndergroundFluidRenderStep extends UniversalRenderStep<UndergroundFluidLocation> {

    public UndergroundFluidRenderStep(UndergroundFluidLocation location) {
        super(location);
        setSize(VP.undergroundFluidSizeChunkX * VP.chunkWidth);
        setFontScale(0.5f);
    }

    @Override
    public void preRender(double topX, double topY, float drawScale, double zoom) {
        setOffset(isJourneyMap ? -blockSize / 2 : 0);
    }

    @Override
    public void draw(double topX, double topY, float drawScale, double zoom) {
        final Minecraft mc = Minecraft.getMinecraft();
        final double regionW = VP.undergroundFluidSizeChunkX * VP.chunkWidth * blockSize;
        final double regionH = VP.undergroundFluidSizeChunkZ * VP.chunkWidth * blockSize;
        // Xaero culling relies on Navigator's visible cache; add centered bounds if that cache grows costly.
        if (!isXaero
                && (topX + regionW < 0 || topY + regionH < 0 || topX > mc.displayWidth || topY > mc.displayHeight)) {
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

        if (!isMinimap() && getZoomStep() >= Config.minZoomLevelForUndergroundFluidDetails - 3) {
            String title = I18n.format("visualprospecting.empty");
            String values = null;
            if (maxAmountInField > 0) {
                title = location.getFluid().getLocalizedName();
                values = MessageFormat.format(
                        "{0}-{1} L/Op",
                        formatAmount(location.getMinProduction()),
                        formatAmount(maxAmountInField));
            }

            int textColor = 0xFFFFFFFF;
            if (UndergroundFluidLayerManager.instance.isSearchActive()) {
                textColor = location.isActive() ? 0xFFFF00 : 0x444444;
            }

            final double labelScale = getMainLabelScale();
            DrawUtils.drawLabel(
                    title,
                    topX + getAdjustedWidth() / 2,
                    topY + 1.5,
                    textColor,
                    0xB4000000,
                    true,
                    labelScale);
            if (values != null) {
                DrawUtils.drawLabel(
                        values,
                        topX + getAdjustedWidth() / 2,
                        topY + 1.5 + (mc.fontRenderer.FONT_HEIGHT + 2) * labelScale,
                        textColor,
                        0xB4000000,
                        true,
                        labelScale);
            }
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
        final double cellLabelScale = getCellLabelScale();
        final double cellLabelOffsetY = isXaero ? 0
                : (mc.fontRenderer.FONT_HEIGHT + 2) * cellLabelScale
                        * getZoomScale(
                                1,
                                0,
                                Config.minZoomLevelForUndergroundFluidDetails,
                                Config.minZoomLevelForUndergroundFluidDetails + 1);
        final double screenW = mc.displayWidth;
        final double screenH = mc.displayHeight;
        setSize(VP.chunkWidth);
        final double cellW = getAdjustedWidth();
        final double cellH = getAdjustedHeight();
        for (int chunkX = 0; chunkX < VP.undergroundFluidSizeChunkX; chunkX++) {
            final double cellX = x + chunkX * cellW;
            if (!isXaero && (cellX + cellW < 0 || cellX > screenW)) continue;
            for (int chunkZ = 0; chunkZ < VP.undergroundFluidSizeChunkZ; chunkZ++) {
                final double cellY = y + chunkZ * cellH;
                if (!isXaero && (cellY + cellH < 0 || cellY > screenH)) continue;
                int amount = chunks[chunkX][chunkZ];
                if (amount <= 0) continue;
                int alpha = productionRange > 1 ? (int) ((amount - minProduction) / productionRange * 255) : 10;
                DrawUtils.drawRect(cellX, cellY, cellW, cellH, fluidColor, alpha);

                if (highlightPeak && amount >= maxProduction) {
                    DrawUtils.drawHollowRect(cellX, cellY, cellW, cellH, 0xFFD700, 204, 1.5);
                }

                if (drawLabels) {
                    DrawUtils.drawLabel(
                            MessageFormat.format("{0} L/Op", formatAmount(amount)),
                            cellX + cellW / 2,
                            cellY + cellH / 2 + cellLabelOffsetY,
                            0xFFFFFFFF,
                            0xB4000000,
                            true,
                            cellLabelScale);
                }
            }
        }
    }

    private String formatAmount(int amount) {
        if (amount < 1000) {
            return String.valueOf(amount);
        }
        final int roundedTenths = (amount + 50) / 100;
        if (roundedTenths % 10 == 0) {
            return (roundedTenths / 10) + "k";
        }
        return (roundedTenths / 10) + "." + (roundedTenths % 10) + "k";
    }

    private double getCellLabelScale() {
        return isXaero ? getFontScale() * 0.7
                : getFontScale() * getZoomScale(1.2, 3, Math.min(Config.minZoomLevelForUndergroundFluidDetails, 4), 5);
    }

    private double getMainLabelScale() {
        return isXaero ? getFontScale()
                : getFontScale()
                        * getZoomScale(1, 4.5, Math.min(Config.minZoomLevelForUndergroundFluidDetails - 3, 4), 5);
    }
}
