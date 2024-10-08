package com.sinthoras.visualprospecting.mixins.late.gregtech;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

import com.sinthoras.visualprospecting.database.ClientCache;

import gregtech.api.items.GTGenericBlock;
import gregtech.common.blocks.BlockOresAbstract;

@Mixin(value = BlockOresAbstract.class, remap = false)
public class GT_Block_Ores_AbstractMixin extends GTGenericBlock {

    protected GT_Block_Ores_AbstractMixin() {
        super(null, null, null);
    }

    @Override
    public boolean onBlockActivated(World world, int blockX, int blockY, int blockZ, EntityPlayer player, int side,
            float offsetX, float offsetY, float offsetZ) {
        ClientCache.instance.onOreInteracted(world, blockX, blockY, blockZ, player);
        return super.onBlockActivated(world, blockX, blockY, blockZ, player, side, offsetX, offsetY, offsetZ);
    }

    @Override
    public void onBlockClicked(World world, int blockX, int blockY, int blockZ, EntityPlayer player) {
        ClientCache.instance.onOreInteracted(world, blockX, blockY, blockZ, player);
        super.onBlockClicked(world, blockX, blockY, blockZ, player);
    }
}
