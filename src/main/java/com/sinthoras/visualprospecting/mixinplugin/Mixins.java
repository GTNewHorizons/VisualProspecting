package com.sinthoras.visualprospecting.mixinplugin;

import static com.sinthoras.visualprospecting.mixinplugin.TargetedMod.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

public enum Mixins {

    // Bartworks mixins
    WorldGenContainerMixin("bartworks.WorldGenContainerMixin", BARTWORKS),

    // Galactic greg mixins
    GT_Worldgenerator_SpaceMixin("galacticgreg.GT_Worldgenerator_SpaceMixin", GALACTICGREG),

    // Gregtech mixins
    GT_Block_Ores_AbstractMixin("gregtech.GT_Block_Ores_AbstractMixin", GT5U),
    GT_MetaTileEntity_AdvSeismicProspectorMixin("gregtech.GT_MetaTileEntity_AdvSeismicProspectorMixin", GT5U),
    GT_MetaTileEntity_ScannerMixin("gregtech.GT_MetaTileEntity_ScannerMixin", GT5U),
    GT_WorldGenContainerMixin("gregtech.WorldGenContainerMixin", GT5U),

    // Journeymap mixins
    DisplayVarsAccessorMixin("journeymap.DisplayVarsAccessor", Side.CLIENT, JOURNEYMAP),
    FullscreenAccessorMixin("journeymap.FullscreenAccessor", Side.CLIENT, JOURNEYMAP),
    FullscreenMixin("journeymap.FullscreenMixin", Side.CLIENT, JOURNEYMAP),
    FullscreenActionsMixin("journeymap.FullscreenActionsMixin", Side.CLIENT, JOURNEYMAP),
    MiniMapMixin("journeymap.MiniMapMixin", Side.CLIENT, JOURNEYMAP),
    RenderWaypointBeaconMixin("journeymap.RenderWaypointBeaconMixin", Side.CLIENT, JOURNEYMAP),
    WaypointManagerMixin("journeymap.WaypointManagerMixin", Side.CLIENT, JOURNEYMAP),

    // TC node tracker mixins
    GuiMainMixin("tcnodetracker.GuiMainMixin", Side.CLIENT, TCNODETRACKER),

    // Xaerosworldmap & Xaerosminimap mixins
    GuiMapMixin("xaerosworldmap.GuiMapMixin", Side.CLIENT, XAEROWORLDMAP),
    WaypointsIngameRendererMixin("xaerosminimap.WaypointsIngameRendererMixin", Side.CLIENT, XAEROMINIMAP),
    MinimapRendererMixin("xaerosminimap.MinimapRendererMixin", Side.CLIENT, XAEROMINIMAP, XAEROWORLDMAP),

    // Vanilla Mixins
    MinecraftServerAccessorMixin("minecraft.MinecraftServerAccessor", Phase.EARLY, Side.BOTH, VANILLA),
    ForgeHooksClientMixin("minecraft.ForgeHooksClientMixin", Phase.EARLY, Side.CLIENT, XAEROMINIMAP, XAEROWORLDMAP),
    ItemEditableBookMixin("minecraft.ItemEditableBookMixin", Phase.EARLY, VANILLA);

    public final String mixinClass;
    public final Phase phase;
    private final Side side;
    private final List<TargetedMod> targetedMods;

    Mixins(String mixinClass, TargetedMod... targetedMods) {
        this.mixinClass = mixinClass;
        this.phase = Phase.LATE;
        this.side = Side.BOTH;
        this.targetedMods = Arrays.asList(targetedMods);
    }

    Mixins(String mixinClass, Phase phase, TargetedMod... targetedMods) {
        this.mixinClass = mixinClass;
        this.phase = phase;
        this.side = Side.BOTH;
        this.targetedMods = Arrays.asList(targetedMods);
    }

    Mixins(String mixinClass, Side side, TargetedMod... targetedMods) {
        this.mixinClass = mixinClass;
        this.phase = Phase.LATE;
        this.side = side;
        this.targetedMods = Arrays.asList(targetedMods);
    }

    Mixins(String mixinClass, Phase phase, Side side, TargetedMod... targetedMods) {
        this.mixinClass = mixinClass;
        this.phase = phase;
        this.side = side;
        this.targetedMods = Arrays.asList(targetedMods);
    }

    public boolean shouldLoad(Set<String> loadedCoreMods, Set<String> loadedMods) {
        return shouldLoadSide() && allModsLoaded(targetedMods, loadedCoreMods, loadedMods);
    }

    private boolean shouldLoadSide() {
        return (side == Side.BOTH || (side == Side.SERVER && FMLLaunchHandler.side().isServer())
                || (side == Side.CLIENT && FMLLaunchHandler.side().isClient()));
    }

    private boolean allModsLoaded(List<TargetedMod> targetedMods, Set<String> loadedCoreMods, Set<String> loadedMods) {
        if (targetedMods.isEmpty()) return false;
        for (TargetedMod target : targetedMods) {
            if (target == TargetedMod.VANILLA) continue;
            // Check coremod first
            if (!loadedCoreMods.isEmpty() && target.coreModClass != null
                    && !loadedCoreMods.contains(target.coreModClass)) {
                return false;
            } else if (!loadedMods.isEmpty() && target.modId != null && !loadedMods.contains(target.modId)) {
                return false;
            }
        }
        return true;
    }

    enum Side {
        BOTH,
        CLIENT,
        SERVER
    }

    public enum Phase {
        EARLY,
        LATE
    }

}
