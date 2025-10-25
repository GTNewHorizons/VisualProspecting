package com.sinthoras.visualprospecting.integration.model.locations;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.gtnewhorizons.navigator.api.model.locations.IWaypointAndLocationProvider;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;

import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TextureSet;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.IOreMaterial;
import gregtech.api.util.LightingHelper;

public class OreVeinLocation implements IWaypointAndLocationProvider {

    private static final String depletedHint = EnumChatFormatting.RED + I18n.format("visualprospecting.depleted");
    private static final String activeWaypointHint = EnumChatFormatting.GOLD
            + I18n.format("visualprospecting.iswaypoint");
    private static final String toggleDepletedHint = EnumChatFormatting.DARK_GRAY + I18n
            .format("visualprospecting.node.deletehint", Keyboard.getKeyName(NavigatorApi.ACTION_KEY.getKeyCode()));

    private final OreVeinPosition oreVeinPosition;
    private final String name;
    private final List<String> materialNames;

    private boolean isActiveAsWaypoint;

    public OreVeinLocation(OreVeinPosition oreVeinPosition) {
        this.oreVeinPosition = oreVeinPosition;
        name = EnumChatFormatting.WHITE + oreVeinPosition.veinType.getVeinName();
        materialNames = oreVeinPosition.veinType.getOreMaterialNames().stream()
                .map(materialName -> EnumChatFormatting.GRAY + materialName).collect(Collectors.toList());
    }

    @Override
    public Waypoint toWaypoint() {
        return new Waypoint(
                oreVeinPosition.getBlockX(),
                65,
                oreVeinPosition.getBlockZ(),
                oreVeinPosition.dimensionId,
                I18n.format("visualprospecting.tracked", oreVeinPosition.veinType.getVeinName()),
                getColor());
    }

    @Override
    public boolean isActiveAsWaypoint() {
        return isActiveAsWaypoint;
    }

    @Override
    public void onWaypointCleared() {
        isActiveAsWaypoint = false;
    }

    @Override
    public void onWaypointUpdated(Waypoint waypoint) {
        isActiveAsWaypoint = waypoint.dimensionId == oreVeinPosition.dimensionId
                && waypoint.blockX == oreVeinPosition.getBlockX()
                && waypoint.blockZ == oreVeinPosition.getBlockZ();
    }

    public void toggleOreVein() {
        ClientCache.instance.toggleOreVein(oreVeinPosition.dimensionId, oreVeinPosition.chunkX, oreVeinPosition.chunkZ);
    }

    @Override
    public int getDimensionId() {
        return oreVeinPosition.dimensionId;
    }

    @Override
    public double getBlockX() {
        return oreVeinPosition.getBlockX() + 0.5;
    }

    @Override
    public double getBlockZ() {
        return oreVeinPosition.getBlockZ() + 0.5;
    }

    public boolean isDepleted() {
        return oreVeinPosition.isDepleted();
    }

    public String getDepletedHint() {
        return depletedHint;
    }

    public String getActiveWaypointHint() {
        return activeWaypointHint;
    }

    public String getName() {
        return name;
    }

    public String getToggleDepletedHint() {
        return toggleDepletedHint;
    }

    public List<String> getMaterialNames() {
        return materialNames;
    }

    public boolean drawSearchHighlight() {
        return oreVeinPosition.veinType.isHighlighted();
    }

    public int getColor() {
        return LightingHelper.getColor(getRepresentativeOre().getRGBA());
    }

    public IIconContainer getIconFromRepresentativeOre() {
        TextureSet textureSet = getRepresentativeOre().getTextureSet();

        return textureSet.mTextures[OrePrefixes.ore.getTextureIndex()];
    }

    public IOreMaterial getRepresentativeOre() {
        return oreVeinPosition.veinType.representativeOre;
    }
}
