package com.sinthoras.visualprospecting.mixinplugin;

import static com.sinthoras.visualprospecting.mixinplugin.TargetedMod.*;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // spotless:off
    // Bartworks mixins
    WorldGenContainerMixin("bartworks.WorldGenContainerMixin", BARTWORKS),

    // Galactic greg mixins
    GT_Worldgenerator_SpaceMixin("galacticgreg.GT_Worldgenerator_SpaceMixin", GALACTICGREG),

    // Gregtech mixins
    GT_Block_Ores_AbstractMixin("gregtech.GT_Block_Ores_AbstractMixin", GT5U),
    GT_MetaTileEntity_AdvSeismicProspectorMixin("gregtech.GT_MetaTileEntity_AdvSeismicProspectorMixin", GT5U),
    GT_MetaTileEntity_ScannerMixin("gregtech.GT_MetaTileEntity_ScannerMixin", GT5U),
    GT_WorldGenContainerMixin("gregtech.WorldGenContainerMixin", GT5U),

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
