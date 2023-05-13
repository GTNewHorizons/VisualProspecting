package com.sinthoras.visualprospecting.mixins.late.journeymap;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.sinthoras.visualprospecting.integration.journeymap.JourneyMapState;
import com.sinthoras.visualprospecting.integration.journeymap.waypoints.WaypointManager;

import journeymap.client.model.Waypoint;
import journeymap.client.render.ingame.RenderWaypointBeacon;

@Mixin(RenderWaypointBeacon.class)
public class RenderWaypointBeaconMixin {

    @Shadow(remap = false)
    static Minecraft mc;

    @Shadow(remap = false)
    static void doRender(Waypoint waypoint) {
        throw new IllegalStateException("Mixin failed to shadow doRender()");
    }

    @Inject(
            method = "renderAll",
            at = @At(
                    value = "INVOKE",
                    target = "Ljourneymap/client/waypoint/WaypointStore;instance()Ljourneymap/client/waypoint/WaypointStore;"),
            remap = false,
            require = 1)
    private static void visualprospecting$onRenderAll(CallbackInfo ci) {
        for (WaypointManager waypointManager : JourneyMapState.instance.waypointManagers) {
            if (waypointManager.hasWaypoint()) {
                final Waypoint waypoint = waypointManager.getJmWaypoint();
                if (waypoint.getDimensions().contains(mc.thePlayer.dimension)) {
                    doRender(waypoint);
                }
            }
        }
    }
}
