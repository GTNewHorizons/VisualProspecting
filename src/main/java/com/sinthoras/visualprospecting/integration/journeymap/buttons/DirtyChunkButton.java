package com.sinthoras.visualprospecting.integration.journeymap.buttons;

import com.sinthoras.visualprospecting.integration.model.buttons.DirtyChunkButtonManager;

public class DirtyChunkButton extends LayerButton {

    public static final DirtyChunkButton instance = new DirtyChunkButton();

    public DirtyChunkButton() {
        super(DirtyChunkButtonManager.instance);
    }
}
