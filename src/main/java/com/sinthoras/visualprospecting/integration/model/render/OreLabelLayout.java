package com.sinthoras.visualprospecting.integration.model.render;

final class OreLabelLayout {

    private OreLabelLayout() {}

    static double staggerOffset(double blockX, double zoomStep, double labelHeight) {
        // Ore columns are three chunks wide. Floor keeps alternation continuous across X = 0
        return zoomStep <= 1 && (((long) Math.floor(blockX / 48)) & 1) != 0 ? labelHeight + 1 : 0;
    }
}
