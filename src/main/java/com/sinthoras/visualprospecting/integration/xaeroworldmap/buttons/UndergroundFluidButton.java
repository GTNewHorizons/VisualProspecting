package com.sinthoras.visualprospecting.integration.xaeroworldmap.buttons;

import com.gtnewhorizons.navigator.api.xaero.buttons.XaeroLayerButton;
import com.sinthoras.visualprospecting.integration.model.buttons.UndergroundFluidButtonManager;

public class UndergroundFluidButton extends XaeroLayerButton {

    public static final UndergroundFluidButton instance = new UndergroundFluidButton();

    public UndergroundFluidButton() {
        super(UndergroundFluidButtonManager.instance);
    }
}
