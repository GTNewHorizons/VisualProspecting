package com.sinthoras.visualprospecting.integration.xaeroworldmap.rendersteps;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiScreen;

public interface RenderStep {
    void draw(@Nullable GuiScreen gui, double cameraX, double cameraZ, double scale);
}
