package com.sinthoras.visualprospecting.mixins.late.tcnodetracker;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.libraries.org.objectweb.asm.Opcodes;

import com.dyonovan.tcnodetracker.gui.GuiMain;
import com.dyonovan.tcnodetracker.lib.AspectLoc;
import com.sinthoras.visualprospecting.integration.model.layers.ThaumcraftNodeLayerManager;
import com.sinthoras.visualprospecting.integration.model.waypoints.Waypoint;

@Mixin(GuiMain.class)
public class GuiMainMixin {

    @Shadow(remap = false)
    private int low;

    @Inject(
            method = "actionPerformed",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/dyonovan/tcnodetracker/TCNodeTracker;zMarker:I",
                    opcode = Opcodes.PUTSTATIC,
                    shift = At.Shift.AFTER,
                    remap = false),
            require = 1,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            cancellable = true)
    private void visualprospecting$onWaypointSet(GuiButton button, CallbackInfo ci, int i) {
        final AspectLoc aspect = GuiMain.aspectList.get(low + i);
        GuiMain.aspectList.clear();
        ThaumcraftNodeLayerManager.instance.setActiveWaypoint(
                new Waypoint(
                        aspect.x,
                        aspect.y,
                        aspect.z,
                        aspect.dimID,
                        I18n.format("visualprospecting.tracked", I18n.format("tile.blockAiry.0.name")),
                        0xFFFFFF));
        ci.cancel();
    }

    @Inject(
            method = "actionPerformed",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/dyonovan/tcnodetracker/TCNodeTracker;doGui:Z",
                    opcode = Opcodes.PUTSTATIC,
                    ordinal = 0,
                    remap = false),
            require = 1)
    private void visualprospecting$onWaypointClear(CallbackInfo ci) {
        ThaumcraftNodeLayerManager.instance.clearActiveWaypoint();
    }

    @Inject(
            method = "actionPerformed",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/dyonovan/tcnodetracker/TCNodeTracker;doGui:Z",
                    opcode = Opcodes.PUTSTATIC,
                    ordinal = 1,
                    remap = false),
            require = 1,
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void visualprospecting$onWaypointDelete(GuiButton button, CallbackInfo ci, int i, int k, int j) {
        ThaumcraftNodeLayerManager.instance.clearActiveWaypoint();
    }
}
