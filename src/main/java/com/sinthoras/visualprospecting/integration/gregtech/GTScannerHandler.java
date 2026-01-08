package com.sinthoras.visualprospecting.integration.gregtech;

import static gregtech.api.util.GTUtility.ItemNBT.setNBT;

import java.util.List;

import javax.annotation.Nonnull;

import gregtech.loaders.postload.ScannerHandlerLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;

import gregtech.api.enums.ItemList;
import gregtech.api.util.GTScannerResult;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.basic.MTEScanner;

public abstract class GTScannerHandler {

    public static void onPostLoad() {
        // add at start to override default behaviour
        MTEScanner.HANDLERS.addFirst(GTScannerHandler::analyzeProspectorData);
    }

    private static @Nullable GTScannerResult analyzeProspectorData(@Nonnull MTEScanner aScanner,
            @Nonnull ItemStack aInput, @Nullable ItemStack aSpecialSlot, @Nullable FluidStack aFluid) {
        // mimic original functionality
        if (aSpecialSlot != null) return null;
        if (!ItemList.Tool_DataStick.isStackEqual(aInput, false, true)) return null;
        // abort if this doesn't have VP data
        if (!aInput.hasTagCompound() || !GTUtility.ItemNBT.getBookTitle(aInput).equals("Raw Prospection Data")
                || !aInput.getTagCompound().hasKey(Tags.VISUALPROSPECTING_FLAG))
            return null;

        ItemStack output = GTUtility.copyAmount(1, aInput);
        assert output != null;
        GTUtility.ItemNBT.setBookTitle(output, "Analyzed Prospection Data");
        final NBTTagCompound compound = output.getTagCompound();
        final int dimensionId = compound.getInteger(Tags.PROSPECTION_DIMENSION_ID);
        final int blockX = compound.getInteger(Tags.PROSPECTION_BLOCK_X);
        final int blockY = compound.getInteger(Tags.PROSPECTION_BLOCK_Y);
        final int blockZ = compound.getInteger(Tags.PROSPECTION_BLOCK_Z);
        final int blockRadius = compound.getInteger(Tags.PROSPECTION_ORE_RADIUS);
        final int numberOfUndergroundFluids = compound.getInteger(Tags.PROSPECTION_NUMBER_OF_UNDERGROUND_FLUID);
        final String position = "Dim: " + dimensionId + " X: " + blockX + " Y: " + blockY + " Z: " + blockZ;

        final NBTTagList bookPages = new NBTTagList();

        final String frontPage = "Prospector report\n" + position
                + "\n\n"
                + "Fluids: "
                + numberOfUndergroundFluids
                + "\n\n"
                + "Ores within "
                + blockRadius
                + " blocks\n\n"
                + "Location is center of orevein\n\n"
                + "Results are synchronized to your map";
        bookPages.appendTag(new NBTTagString(frontPage));

        final List<OreVeinPosition> foundOreVeins = ServerCache.instance
                .prospectOreBlockRadius(dimensionId, blockX, blockZ, blockRadius);
        if (!foundOreVeins.isEmpty()) {
            final int pageSize = 7;
            final int numberOfPages = (foundOreVeins.size() + pageSize) / pageSize; // Equals to
            // ceil((foundOreVeins.size())

            for (int pageNumber = 0; pageNumber < numberOfPages; pageNumber++) {
                final StringBuilder pageString = new StringBuilder();
                for (int i = 0; i < pageSize; i++) {
                    final int veinId = pageNumber * pageSize + i;
                    if (veinId < foundOreVeins.size()) {
                        final OreVeinPosition oreVein = foundOreVeins.get(veinId);
                        pageString.append(oreVein.getBlockX()).append(",").append(oreVein.getBlockZ()).append(" - ")
                                .append(oreVein.veinType.getVeinName()).append("\n");
                    }
                }
                String pageCounter = numberOfPages > 1 ? String.format(" %d/%d", pageNumber + 1, numberOfPages) : "";
                NBTTagString pageTag = new NBTTagString(String.format("Ore Veins %s\n\n", pageCounter) + pageString);
                bookPages.appendTag(pageTag);
            }
        }

        if (compound.hasKey(Tags.PROSPECTION_FLUIDS)) {
            GTUtility.ItemNBT.fillBookWithList(
                    bookPages,
                    "Fluids%s\n\n",
                    "\n",
                    9,
                    compound.getString(Tags.PROSPECTION_FLUIDS).split("\\|"));

            final String fluidCoverPage = "Fluid notes\n\n" + "Prospects from NW to SE 576 chunks"
                    + "(9 8x8 fields)\n around and gives min-max amount"
                    + "\n\n"
                    + "[1][2][3]"
                    + "\n"
                    + "[4][5][6]"
                    + "\n"
                    + "[7][8][9]"
                    + "\n"
                    + "\n"
                    + "[5] - Prospector in this 8x8 area";
            bookPages.appendTag(new NBTTagString(fluidCoverPage));

            String tFluidsPosStr = "X: " + Math.floorDiv(blockX, 16 * 8) * 16 * 8
                    + " Z: "
                    + Math.floorDiv(blockZ, 16 * 8) * 16 * 8
                    + "\n";
            int xOff = blockX - Math.floorDiv(blockX, 16 * 8) * 16 * 8;
            xOff = xOff / 16;
            int xOffRemain = 7 - xOff;

            int zOff = blockZ - Math.floorDiv(blockZ, 16 * 8) * 16 * 8;
            zOff = zOff / 16;
            int zOffRemain = 7 - zOff;

            for (; zOff > 0; zOff--) {
                tFluidsPosStr = tFluidsPosStr.concat("--------\n");
            }
            for (; xOff > 0; xOff--) {
                tFluidsPosStr = tFluidsPosStr.concat("-");
            }

            tFluidsPosStr = tFluidsPosStr.concat("P");

            for (; xOffRemain > 0; xOffRemain--) {
                tFluidsPosStr = tFluidsPosStr.concat("-");
            }
            tFluidsPosStr = tFluidsPosStr.concat("\n");
            for (; zOffRemain > 0; zOffRemain--) {
                tFluidsPosStr = tFluidsPosStr.concat("--------\n");
            }
            tFluidsPosStr = tFluidsPosStr.concat(
                    " X: " + (Math.floorDiv(blockX, 16 * 8) + 1) * 16 * 8
                            + " Z: "
                            + (Math.floorDiv(blockZ, 16 * 8) + 1) * 16 * 8); // +1 field to find bottomright of [5]
            final String fluidsPage = "Corners of [5] are \n" + tFluidsPosStr + "\n" + "P - Prospector in 8x8 field";
            bookPages.appendTag(new NBTTagString(fluidsPage));
        }

        compound.setString("author", position);
        compound.setTag("pages", bookPages);
        setNBT(output, compound);

        // Mimic original behaviour
        return new GTScannerResult(ScannerHandlerLoader.SCAN_PROSPECTING_DATA_EUT, ScannerHandlerLoader.SCAN_PROSPECTING_DATA_DURATION, 1, 0, 0, output, true);
    }
}
