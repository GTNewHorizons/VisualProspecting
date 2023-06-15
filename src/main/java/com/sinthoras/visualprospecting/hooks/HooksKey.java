package com.sinthoras.visualprospecting.hooks;

import static com.sinthoras.visualprospecting.Utils.isTCNodeTrackerInstalled;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.sinthoras.visualprospecting.integration.model.buttons.OreVeinButtonManager;
import com.sinthoras.visualprospecting.integration.model.buttons.ThaumcraftNodeButtonManager;
import com.sinthoras.visualprospecting.integration.model.buttons.UndergroundFluidButtonManager;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class HooksKey {

    private final KeyBinding keyToggleOres = new KeyBinding(
            "visualprospecting.key.toggleore.name",
            Keyboard.KEY_NONE,
            "visualprospecting.key.action.category");
    private final KeyBinding keyToggleFluids = new KeyBinding(
            "visualprospecting.key.togglefluid.name",
            Keyboard.KEY_NONE,
            "visualprospecting.key.action.category");
    private final KeyBinding keyToggleNodes = new KeyBinding(
            "visualprospecting.key.togglenode.name",
            Keyboard.KEY_NONE,
            "visualprospecting.key.action.category");

    public HooksKey() {
        FMLCommonHandler.instance().bus().register(this);
        ClientRegistry.registerKeyBinding(keyToggleOres);
        ClientRegistry.registerKeyBinding(keyToggleFluids);
        if (isTCNodeTrackerInstalled()) {
            ClientRegistry.registerKeyBinding(keyToggleNodes);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void key(InputEvent.KeyInputEvent event) {
        checkAndToggleOverlays();
    }

    private void checkAndToggleOverlays() {
        if (keyToggleOres.isPressed()) {
            OreVeinButtonManager.instance.toggle();
        }
        if (keyToggleFluids.isPressed()) {
            UndergroundFluidButtonManager.instance.toggle();
        }
        if (keyToggleNodes.isPressed() && isTCNodeTrackerInstalled()) {
            ThaumcraftNodeButtonManager.instance.toggle();
        }
    }
}
