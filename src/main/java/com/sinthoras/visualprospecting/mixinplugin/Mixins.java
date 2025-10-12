package com.sinthoras.visualprospecting.mixinplugin;

import static com.sinthoras.visualprospecting.mixinplugin.TargetedMod.*;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // spotless:off
    // Gregtech mixins
    MTEAdvSeismicProspectorMixin("gregtech.MTEAdvSeismicProspectorMixin", GT5U),
    MTEScannerMixin("gregtech.MTEScannerMixin", GT5U),

    // Vanilla Mixins
    MINECRAFT(new MixinBuilder()
            .addCommonMixins(
                    "minecraft.MinecraftServerAccessor",
                    "minecraft.ItemEditableBookMixin")
            .setPhase(Phase.EARLY));
    // spotless:on

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    Mixins(String mixinClass, TargetedMod requiredMod) {
        this.builder = new MixinBuilder().addCommonMixins(mixinClass).addRequiredMod(requiredMod).setPhase(Phase.LATE);
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}
