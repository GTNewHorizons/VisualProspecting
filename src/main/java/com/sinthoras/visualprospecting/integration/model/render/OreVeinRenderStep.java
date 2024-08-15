package com.sinthoras.visualprospecting.integration.model.render;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.gtnewhorizons.navigator.api.model.steps.UniversalInteractableStep;
import com.gtnewhorizons.navigator.api.util.DrawUtils;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.integration.model.locations.OreVeinLocation;

public class OreVeinRenderStep extends UniversalInteractableStep<OreVeinLocation> {

    private static final ResourceLocation depletedTextureLocation = new ResourceLocation(
            Tags.MODID,
            "textures/depleted.png");
    private static final int fontHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;

    public OreVeinRenderStep(OreVeinLocation location) {
        super(location);
        setFontScale(1.2f);
        setMinScale(1);
    }

    @Override
    public void preRender(double topX, double topY, float drawScale, double zoom) {
        double iconSize = isXaero ? 10 * zoom : 32 * drawScale;
        setSize(iconSize);
        setOffset(-iconSize / 2);
    }

    @Override
    public void draw(double topX, double topY, float drawScale, double zoom) {
        if (zoom >= Config.minZoomLevelForOreLabel && !location.isDepleted()) {
            final int fontColor = location.drawSearchHighlight() ? 0xFFFFFF : 0x7F7F7F;
            DrawUtils.drawLabel(
                    location.getName(),
                    topX + width / 2,
                    topY - fontHeight - 5,
                    fontColor,
                    0,
                    true,
                    getFontScale());
        }

        final IIcon blockIcon = Blocks.stone.getIcon(0, 0);
        DrawUtils.drawQuad(blockIcon, topX, topY, width, height, 0xFFFFFF, 255);

        DrawUtils.drawQuad(location.getIconFromPrimaryOre(), topX, topY, width, height, location.getColor(), 255);

        if (!location.drawSearchHighlight() || location.isDepleted()) {
            DrawUtils.drawRect(topX, topY, width, height, 0x000000, 150);
            if (location.isDepleted()) {
                DrawUtils.drawQuad(depletedTextureLocation, topX, topY, width, height, 0xFFFFFF, 255);
            }
        }

        if (location.isActiveAsWaypoint()) {
            final double thickness = width / 8;
            DrawUtils.drawHollowRect(topX, topY, width, height, 0xFFD700, 204, thickness);
        }
    }

    @Override
    public void getTooltip(List<String> list) {
        if (location.isDepleted()) {
            list.add(location.getDepletedHint());
        }
        if (location.isActiveAsWaypoint()) {
            list.add(location.getActiveWaypointHint());
        }
        list.add(location.getName());
        if (!location.isDepleted()) {
            list.addAll(location.getMaterialNames());
        }
        list.add(location.getToggleDepletedHint());
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height;
    }

    @Override
    public void onActionKeyPressed() {
        location.toggleOreVein();
    }

}
