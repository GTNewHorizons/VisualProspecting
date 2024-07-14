package com.sinthoras.visualprospecting.integration.xaeroworldmap.rendersteps;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.navigator.api.model.locations.IWaypointAndLocationProvider;
import com.gtnewhorizons.navigator.api.util.DrawUtils;
import com.gtnewhorizons.navigator.api.xaero.rendersteps.XaeroInteractableStep;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.integration.model.locations.OreVeinLocation;

public class OreVeinRenderStep implements XaeroInteractableStep {

    private final OreVeinLocation oreVeinLocation;
    private final ResourceLocation depletedTextureLocation = new ResourceLocation(Tags.MODID, "textures/depleted.png");
    private final IIcon blockStoneIcon = Blocks.stone.getIcon(0, 0);
    private double iconSize;
    private double iconX;
    private double iconY;

    public OreVeinRenderStep(OreVeinLocation veinPosition) {
        oreVeinLocation = veinPosition;
    }

    @Override
    public void draw(@Nullable GuiScreen gui, double cameraX, double cameraZ, double scale) {
        iconSize = 10 * scale;
        final double iconSizeHalf = iconSize / 2;
        final double scaleForGui = Math.max(1, scale);
        iconX = (oreVeinLocation.getBlockX() - 0.5 - cameraX) * scaleForGui - iconSizeHalf;
        iconY = (oreVeinLocation.getBlockZ() - 0.5 - cameraZ) * scaleForGui - iconSizeHalf;

        GL11.glPushMatrix();
        GL11.glTranslated(oreVeinLocation.getBlockX() - 0.5 - cameraX, oreVeinLocation.getBlockZ() - 0.5 - cameraZ, 0);
        GL11.glScaled(1 / scaleForGui, 1 / scaleForGui, 1);
        DrawUtils.drawQuad(blockStoneIcon, -iconSizeHalf, -iconSizeHalf, iconSize, iconSize, 0xFFFFFF, 255);

        DrawUtils.drawQuad(
                oreVeinLocation.getIconFromPrimaryOre(),
                -iconSizeHalf,
                -iconSizeHalf,
                iconSize,
                iconSize,
                oreVeinLocation.getColor(),
                255);

        if (!oreVeinLocation.drawSearchHighlight() || oreVeinLocation.isDepleted()) {
            DrawUtils.drawGradientRect(
                    -iconSizeHalf,
                    -iconSizeHalf,
                    iconSizeHalf,
                    iconSizeHalf,
                    0,
                    0x96000000,
                    0x96000000);
            if (oreVeinLocation.isDepleted()) {
                DrawUtils.drawQuad(
                        depletedTextureLocation,
                        -iconSizeHalf,
                        -iconSizeHalf,
                        iconSize,
                        iconSize,
                        0xFFFFFF,
                        255);
            }
        }

        if (scale >= Utils.journeyMapScaleToLinear(Config.minZoomLevelForOreLabel) && !oreVeinLocation.isDepleted()) {
            final int fontColor = oreVeinLocation.drawSearchHighlight() ? 0xFFFFFFFF : 0xFF7F7F7F;
            String text = I18n.format(oreVeinLocation.getName());
            DrawUtils.drawLabel(
                    text,
                    0,
                    -iconSizeHalf - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT - 5,
                    fontColor,
                    0xB4000000,
                    true,
                    1.6);
        }

        if (oreVeinLocation.isActiveAsWaypoint()) {
            final double thickness = iconSize / 8;
            final int color = 0xFFFFD700;
            DrawUtils.drawGradientRect(
                    -iconSizeHalf - thickness,
                    -iconSizeHalf - thickness,
                    iconSizeHalf,
                    -iconSizeHalf,
                    0,
                    color,
                    color);
            DrawUtils.drawGradientRect(
                    iconSizeHalf,
                    -iconSizeHalf - thickness,
                    iconSizeHalf + thickness,
                    iconSizeHalf,
                    0,
                    color,
                    color);
            DrawUtils.drawGradientRect(
                    -iconSizeHalf,
                    iconSizeHalf,
                    iconSizeHalf + thickness,
                    iconSizeHalf + thickness,
                    0,
                    color,
                    color);
            DrawUtils.drawGradientRect(
                    -iconSizeHalf - thickness,
                    -iconSizeHalf,
                    -iconSizeHalf,
                    iconSizeHalf + thickness,
                    0,
                    color,
                    color);
        }

        GL11.glPopMatrix();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY, double scale) {
        final double scaleForGui = Math.max(1, scale);
        mouseX = mouseX * scaleForGui;
        mouseY = mouseY * scaleForGui;
        return mouseX >= iconX && mouseY >= iconY && mouseX <= iconX + iconSize && mouseY <= iconY + iconSize;
    }

    @Override
    public void getTooltip(List<String> list) {
        if (oreVeinLocation.isDepleted()) {
            list.add(oreVeinLocation.getDepletedHint());
        }

        if (oreVeinLocation.isActiveAsWaypoint()) {
            list.add(oreVeinLocation.getActiveWaypointHint());
        }

        list.add(oreVeinLocation.getName());

        if (!oreVeinLocation.isDepleted()) {
            list.addAll(oreVeinLocation.getMaterialNames());
        }

        list.add(oreVeinLocation.getToggleDepletedHint());
    }

    @Override
    public void drawCustomTooltip(GuiScreen gui, double mouseX, double mouseY, double scale, int scaleAdj) {}

    @Override
    public void onActionButton() {
        oreVeinLocation.toggleOreVein();
    }

    @Override
    public IWaypointAndLocationProvider getLocationProvider() {
        return oreVeinLocation;
    }
}
