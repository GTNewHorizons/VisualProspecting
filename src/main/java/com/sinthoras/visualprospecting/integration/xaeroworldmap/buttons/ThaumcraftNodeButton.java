package com.sinthoras.visualprospecting.integration.xaeroworldmap.buttons;

import com.gtnewhorizons.navigator.api.xaero.buttons.XaeroLayerButton;
import com.sinthoras.visualprospecting.integration.model.buttons.ThaumcraftNodeButtonManager;

public class ThaumcraftNodeButton extends XaeroLayerButton {

    public static final ThaumcraftNodeButton instance = new ThaumcraftNodeButton();

    public ThaumcraftNodeButton() {
        super(ThaumcraftNodeButtonManager.instance);
    }
}
