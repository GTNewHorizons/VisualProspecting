package com.sinthoras.visualprospecting.integration.model.buttons;

import com.gtnewhorizons.navigator.api.model.buttons.ButtonManager;

public class OreVeinButtonManager extends ButtonManager {

    public static final OreVeinButtonManager instance = new OreVeinButtonManager();

    public OreVeinButtonManager() {
        super("visualprospecting.button.orevein", "oreveins");
    }
}
