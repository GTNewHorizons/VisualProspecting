package com.sinthoras.visualprospecting.mixins.late.galacticgreg;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.database.ServerCache;

import galacticgreg.WorldGeneratorSpace;
import gregtech.api.world.GTWorldgen;

@Mixin(WorldGeneratorSpace.class)
public class GT_Worldgenerator_SpaceMixin {

    @Redirect(
            method = "Generate_OreVeins",
            at = @At(
                    value = "INVOKE",
                    target = "Lgregtech/api/world/GTWorldgen;executeWorldgen(Lnet/minecraft/world/World;Ljava/util/Random;Ljava/lang/String;IIILnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/chunk/IChunkProvider;)Z"),
            remap = false,
            require = 1)
    private boolean visualprospecting$onOreVeinGenerated(GTWorldgen worldGen, World world, Random random, String biome,
            int dimensionType, int blockX, int blockZ, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        final boolean oreVeinPlaced = worldGen.executeWorldgen(
                world,
                random,
                biome,
                Integer.MIN_VALUE,
                blockX,
                blockZ,
                chunkGenerator,
                chunkProvider);
        if (oreVeinPlaced) {
            ServerCache.instance.notifyOreVeinGeneration(
                    world.provider.dimensionId,
                    Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(blockX)),
                    Utils.mapToCenterOreChunkCoord(Utils.coordBlockToChunk(blockZ)),
                    worldGen.mWorldGenName);
        }
        return oreVeinPlaced;
    }
}
