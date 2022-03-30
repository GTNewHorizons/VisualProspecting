package com.sinthoras.visualprospecting.integration.model.buttons;

public class DirtyChunkButtonManager extends ButtonManager {

    public static final DirtyChunkButtonManager instance = new DirtyChunkButtonManager();

    public DirtyChunkButtonManager() {
        super("visualprospecting.button.dirtychunk", "nodes");
    }
}
