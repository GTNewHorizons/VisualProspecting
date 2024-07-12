package com.sinthoras.visualprospecting.integration.journeymap.buttons;

import com.gtnewhorizons.navigator.api.journeymap.buttons.JMLayerButton;
import com.sinthoras.visualprospecting.integration.model.buttons.ThaumcraftNodeButtonManager;

public class ThaumcraftNodeButton extends JMLayerButton {

    public static final ThaumcraftNodeButton instance = new ThaumcraftNodeButton();

    public ThaumcraftNodeButton() {
        super(ThaumcraftNodeButtonManager.instance);
    }
}
