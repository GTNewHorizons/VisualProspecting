package com.sinthoras.visualprospecting.mixins.minecraft;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.network.ProspectingNotification;

@Mixin(value = ItemEditableBook.class)
public class ItemEditableBookMixin {

    @Inject(method = "onItemRightClick", at = @At("HEAD"))
    private void visualprospecting$onItemRightClick(ItemStack itemStack, World world, EntityPlayer entityPlayer,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!world.isRemote) {
            final NBTTagCompound compound = itemStack.getTagCompound();
            if (compound.hasKey(Tags.VISUALPROSPECTING_FLAG)) {
                final int dimensionId = compound.getInteger(Tags.PROSPECTION_DIMENSION_ID);
                final int blockX = compound.getInteger(Tags.PROSPECTION_BLOCK_X);
                final int blockZ = compound.getInteger(Tags.PROSPECTION_BLOCK_Z);
                final int blockRadius = compound.getInteger(Tags.PROSPECTION_ORE_RADIUS);
                if (world.provider.dimensionId == dimensionId) {
                    final List<OreVeinPosition> foundOreVeins = ServerCache.instance
                            .prospectOreBlockRadius(dimensionId, blockX, blockZ, blockRadius);
                    final List<UndergroundFluidPosition> foundUndergroundFluids = ServerCache.instance
                            .prospectUndergroundFluidBlockRadius(
                                    world,
                                    blockX,
                                    blockZ,
                                    VP.undergroundFluidChunkProspectingBlockRadius);
                    if (Utils.isLogicalClient()) {
                        ClientCache.instance.putOreVeins(foundOreVeins);
                        ClientCache.instance.putUndergroundFluids(foundUndergroundFluids);
                    } else {
                        VP.network.sendTo(
                                new ProspectingNotification(foundOreVeins, foundUndergroundFluids),
                                (EntityPlayerMP) entityPlayer);
                    }
                }
            }
        }
    }
}
