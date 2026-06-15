package com.sinthoras.visualprospecting.database;

import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import com.sinthoras.visualprospecting.VP;

import cpw.mods.fml.common.Optional;
import gregtech.api.enums.Mods;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.prefab.world.gen.WorldProviderSpace;

/**
 * Resolves a dimension name from a {@code dimensionId}, regardless of if that dimension is currently loaded or not.
 */
public final class DimensionNameResolver {

    private DimensionNameResolver() {}

    public static String resolve(int dimensionId) {
        final WorldServer world = DimensionManager.getWorld(dimensionId);
        WorldProvider provider = world != null ? world.provider : null;
        if (provider == null) {
            try {
                provider = DimensionManager.createProviderFor(dimensionId);
            } catch (Throwable t) {
                VP.LOG.warn("Could not resolve a provider for dimensionId={}: {}", dimensionId, t);
                return "";
            }
        }

        final String celestialName = Mods.GalacticraftCore.isModLoaded() ? getCelestialBodyName(provider) : null;
        if (celestialName != null) return celestialName;

        final String name = provider.getDimensionName();
        return name == null ? "" : name;
    }

    @Optional.Method(modid = Mods.ModIDs.GALACTICRAFT_CORE)
    private static String getCelestialBodyName(WorldProvider provider) {
        if (provider instanceof WorldProviderSpace space) {
            final CelestialBody body = space.getCelestialBody();
            return body == null ? null : body.getName();
        }
        return null;
    }
}
