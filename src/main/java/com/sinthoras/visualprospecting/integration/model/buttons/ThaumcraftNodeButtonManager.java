package com.sinthoras.visualprospecting.integration.model.buttons;

import com.gtnewhorizons.navigator.api.model.buttons.ButtonManager;

public class ThaumcraftNodeButtonManager extends ButtonManager {

    public static final ThaumcraftNodeButtonManager instance = new ThaumcraftNodeButtonManager();

    public ThaumcraftNodeButtonManager() {
        super("visualprospecting.button.nodes", "nodes");
    }
}
