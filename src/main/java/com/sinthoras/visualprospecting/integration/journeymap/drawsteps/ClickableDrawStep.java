package com.sinthoras.visualprospecting.integration.journeymap.drawsteps;

import com.sinthoras.visualprospecting.integration.model.locations.IWaypointAndLocationProvider;
import java.util.List;
import journeymap.client.render.draw.DrawStep;
import net.minecraft.client.gui.FontRenderer;

public interface ClickableDrawStep extends DrawStep {

    List<String> getTooltip();

    void drawTooltip(FontRenderer fontRenderer, int mouseX, int mouseY, int displayWidth, int displayHeight);

    boolean isMouseOver(int mouseX, int mouseY);

    void onActionKeyPressed();

    IWaypointAndLocationProvider getLocationProvider();
}
