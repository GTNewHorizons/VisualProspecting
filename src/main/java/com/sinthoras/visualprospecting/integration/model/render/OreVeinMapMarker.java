package com.sinthoras.visualprospecting.integration.model.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.gtnewhorizons.navigator.api.model.markers.MapMarker;
import com.sinthoras.visualprospecting.Config;
import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.integration.model.locations.OreVeinLocation;

import gregtech.api.interfaces.IIconContainer;
import gregtech.api.util.client.ResourceUtils;

public final class OreVeinMapMarker {

    private static final ResourceLocation DEPLETED = new ResourceLocation(Tags.MODID, "textures/depleted.png");
    private static final ResourceLocation MARKED = new ResourceLocation(Tags.MODID, "textures/node_marked.png");
    private static final int ICON_SIZE = 32;
    private static BufferedImage depletedImage;
    private static BufferedImage markedImage;

    private OreVeinMapMarker() {}

    public static @Nullable MapMarker create(OreVeinLocation location) {
        BufferedImage icon = createIcon(location);
        if (icon == null) return null;

        MapMarker marker = new MapMarker(icon).setDisplaySize(ICON_SIZE, ICON_SIZE).setDisplayZoomScale(1, 2, 3, 5);
        if (!location.isDepleted()) {
            marker.setLabel(location.getName()).setLabelColor(location.drawSearchHighlight() ? 0xFFFFFF : 0x7F7F7F)
                    .setLabelScale(1.2F).setLabelZoomScale(1, 2.5, Math.min(Config.minZoomLevelForOreLabel, 4), 5)
                    .setLabelOffsetY(28).setLabelBackgroundOpacity(0.35F)
                    .setLabelMinZoom(Config.minZoomLevelForOreLabel)
                    .setLabelOnMinimap(Config.showOreLabelsOnJourneyMap6Minimap);
        }
        return marker;
    }

    private static @Nullable BufferedImage createIcon(OreVeinLocation location) {
        BufferedImage background = spriteImage(
                DimensionStoneBackground.getBackgroundIcon(location.getDimensionId()),
                0xFFFFFF);
        IIconContainer ore = location.getIconFromPrimaryOre();
        BufferedImage oreImage = spriteImage(ore.getIcon(), location.getColor());
        if (background == null || oreImage == null) return null;

        BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            draw(graphics, background);
            draw(graphics, oreImage);
            BufferedImage overlay = spriteImage(ore.getOverlayIcon(), 0xFFFFFF);
            if (overlay != null) draw(graphics, overlay);
            if (!location.drawSearchHighlight() || location.isDepleted()) {
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.setColor(new Color(0, 0, 0, 150));
                graphics.fillRect(0, 0, ICON_SIZE, ICON_SIZE);
            }
            if (location.isDepleted()) draw(graphics, getDepletedImage());
            if (location.isActiveAsWaypoint()) draw(graphics, getMarkedImage());
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void draw(Graphics2D graphics, @Nullable BufferedImage image) {
        if (image != null) graphics.drawImage(image, 0, 0, ICON_SIZE, ICON_SIZE, null);
    }

    private static @Nullable BufferedImage spriteImage(@Nullable IIcon icon, int tint) {
        if (icon == null) return null;
        BufferedImage source = loadSprite(icon);
        if (source == null) source = atlasSpriteImage(icon);
        if (source == null) return null;

        int red = tint >> 16 & 0xFF;
        int green = tint >> 8 & 0xFF;
        int blue = tint & 0xFF;
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            pixels[i] = pixel & 0xFF000000 | (pixel >> 16 & 0xFF) * red / 255 << 16
                    | (pixel >> 8 & 0xFF) * green / 255 << 8
                    | (pixel & 0xFF) * blue / 255;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private static @Nullable BufferedImage loadSprite(IIcon icon) {
        ResourceLocation location = ResourceUtils.getCompleteBlockTextureResourceLocation(icon.getIconName());
        try (InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(location)
                .getInputStream()) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) return null;
            int frameHeight = Math.min(image.getWidth(), image.getHeight());
            return image.getSubimage(0, 0, image.getWidth(), frameHeight);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static @Nullable BufferedImage atlasSpriteImage(IIcon icon) {
        if (!(icon instanceof TextureAtlasSprite sprite) || sprite.getFrameCount() == 0) return null;

        int sourceWidth = sprite.getIconWidth();
        int sourceHeight = sprite.getIconHeight();
        int[][] frame = sprite.getFrameTextureData(0);
        if (frame.length == 0 || frame[0] == null || frame[0].length < sourceWidth * sourceHeight) return null;

        // The atlas location is irrelevant here; reuse MapMarker's normalized sprite region to exclude padding.
        MapMarker spriteRegion = new MapMarker(TextureMap.locationBlocksTexture, sprite);
        int width = spriteRegion.getTextureWidth();
        int height = spriteRegion.getTextureHeight();
        int offsetX = spriteRegion.getTextureX() - sprite.getOriginX();
        int offsetY = spriteRegion.getTextureY() - sprite.getOriginY();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, frame[0], offsetY * sourceWidth + offsetX, sourceWidth);
        return image;
    }

    private static @Nullable BufferedImage getDepletedImage() {
        if (depletedImage == null) depletedImage = load(DEPLETED);
        return depletedImage;
    }

    private static @Nullable BufferedImage getMarkedImage() {
        if (markedImage == null) markedImage = load(MARKED);
        return markedImage;
    }

    private static @Nullable BufferedImage load(ResourceLocation location) {
        try (InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(location)
                .getInputStream()) {
            return ImageIO.read(stream);
        } catch (IOException e) {
            VP.LOG.error("Could not load map marker image " + location, e);
            return null;
        }
    }
}
