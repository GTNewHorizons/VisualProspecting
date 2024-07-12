package com.sinthoras.visualprospecting.integration.journeymap.buttons;

import com.gtnewhorizons.navigator.api.journeymap.buttons.JMLayerButton;
import com.sinthoras.visualprospecting.integration.model.buttons.UndergroundFluidButtonManager;

public class UndergroundFluidButton extends JMLayerButton {

    public static final UndergroundFluidButton instance = new UndergroundFluidButton();

    public UndergroundFluidButton() {
        super(UndergroundFluidButtonManager.instance);
    }
}
