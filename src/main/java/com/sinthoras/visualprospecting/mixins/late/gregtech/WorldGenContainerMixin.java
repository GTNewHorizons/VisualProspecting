package com.sinthoras.visualprospecting.mixins.late.gregtech;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.ServerCache;

import gregtech.common.GTWorldgenerator;
import gregtech.common.WorldgenGTOreLayer;

@Mixin(GTWorldgenerator.WorldGenContainer.class)
public class WorldGenContainerMixin {

    // Redirect both calls to ensure that Bartworks ore veins are captured as well
    @Redirect(
            method = "worldGenFindVein",
            at = @At(
                    value = "INVOKE",
                    target = "Lgregtech/common/WorldgenGTOreLayer;executeWorldgenChunkified(Lnet/minecraft/world/World;Ljava/util/Random;Ljava/lang/String;IIIIILnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/chunk/IChunkProvider;)I"),
            remap = false,
            require = 2)
    protected int visualprospecting$onOreVeinPlaced(WorldgenGTOreLayer instance, World aWorld, Random aRandom,
            String aBiome, int aDimensionType, int aChunkX, int aChunkZ, int aSeedX, int aSeedZ,
            IChunkProvider aChunkGenerator, IChunkProvider aChunkProvider) {
        final int result = instance.executeWorldgenChunkified(
                aWorld,
                aRandom,
                aBiome,
                aDimensionType,
                aChunkX,
                aChunkZ,
                aSeedX,
                aSeedZ,
                aChunkGenerator,
                aChunkProvider);
        if (result == WorldgenGTOreLayer.ORE_PLACED && !instance.mWorldGenName.equals("NoOresInVein")) {
            ServerCache.instance.notifyOreVeinGeneration(
                    aWorld.provider.dimensionId,
                    Utils.coordBlockToChunk(aSeedX),
                    Utils.coordBlockToChunk(aSeedZ),
                    instance.mWorldGenName);
        }
        return result;
    }
}
