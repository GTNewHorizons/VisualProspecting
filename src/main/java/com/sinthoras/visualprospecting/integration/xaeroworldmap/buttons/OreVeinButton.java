package com.sinthoras.visualprospecting.integration.xaeroworldmap.buttons;

import com.gtnewhorizons.navigator.api.xaero.buttons.XaeroLayerButton;
import com.sinthoras.visualprospecting.integration.model.buttons.OreVeinButtonManager;

public class OreVeinButton extends XaeroLayerButton {

    public static final OreVeinButton instance = new OreVeinButton();

    public OreVeinButton() {
        super(OreVeinButtonManager.instance);
    }
}
