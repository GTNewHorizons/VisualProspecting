package com.sinthoras.visualprospecting.integration.journeymap.buttons;

import com.gtnewhorizons.navigator.api.journeymap.buttons.JMLayerButton;
import com.sinthoras.visualprospecting.integration.model.buttons.OreVeinButtonManager;

public class OreVeinButton extends JMLayerButton {

    public static final OreVeinButton instance = new OreVeinButton();

    public OreVeinButton() {
        super(OreVeinButtonManager.instance);
    }
}
