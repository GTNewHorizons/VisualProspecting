package com.sinthoras.visualprospecting.integration.model.buttons;

import com.gtnewhorizons.navigator.api.model.buttons.ButtonManager;

public class UndergroundFluidButtonManager extends ButtonManager {

    public static final UndergroundFluidButtonManager instance = new UndergroundFluidButtonManager();

    public UndergroundFluidButtonManager() {
        super("visualprospecting.button.undergroundfluid", "undergroundfluid");
    }
}
