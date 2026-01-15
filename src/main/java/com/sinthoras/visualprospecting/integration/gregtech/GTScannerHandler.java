package com.sinthoras.visualprospecting.integration.gregtech;

import static gregtech.api.util.GTUtility.ItemNBT.getProspectionFrontPage;
import static gregtech.api.util.GTUtility.ItemNBT.getProspectionGridPage;
import static gregtech.api.util.GTUtility.ItemNBT.getProspectionOilLocationPage;
import static gregtech.api.util.GTUtility.ItemNBT.getProspectionOilPosStr;
import static gregtech.api.util.GTUtility.ItemNBT.setNBT;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fluids.FluidStack;

import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.ServerCache;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTScannerResult;
import gregtech.api.util.GTUtility;
import gregtech.loaders.postload.ScannerHandlerLoader;

public abstract class GTScannerHandler {

    public static void onPostLoad() {
        // add at start to override default behaviour
        RecipeMaps.scannerHandlers.addFirst(GTScannerHandler::analyzeProspectorData);
    }

    private static @Nullable GTScannerResult analyzeProspectorData(@Nonnull MetaTileEntity aScanner,
            @Nonnull ItemStack aInput, @Nullable ItemStack aSpecialSlot, @Nullable FluidStack aFluid) {

        // validate inputs
        if (!ScannerHandlerLoader.isValidProspectionBook(aInput, aSpecialSlot)) return null;
        // ensure it has the right flag
        if (!aInput.getTagCompound().hasKey(Tags.VISUALPROSPECTING_FLAG)) return null;

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
        final String position = GTUtility.ItemNBT.getProspectionBookTitle(dimensionId, blockX, blockY, blockZ);

        // generate starting pages
        final NBTTagList bookPages = new NBTTagList();
        final String frontPage = getProspectionFrontPage(
                position,
                GTUtility.formatNumbers(numberOfUndergroundFluids),
                GTUtility.formatNumbers(blockRadius)).replace("\nOils: ", "\nFluids: ")
                        .replace("\nCheck NEI to confirm orevein type", "\nResults are synchronized to your map");
        bookPages.appendTag(new NBTTagString(frontPage));

        // append ores
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

        // append fluids
        if (compound.hasKey(Tags.PROSPECTION_FLUIDS)) {
            GTUtility.ItemNBT.fillBookWithList(
                    bookPages,
                    "Fluids%s\n\n",
                    "\n",
                    9,
                    compound.getString(Tags.PROSPECTION_FLUIDS).split("\\|"));

            bookPages.appendTag(new NBTTagString(getProspectionGridPage().replace("Oil notes", "Fluid notes")));
            bookPages.appendTag(
                    new NBTTagString(getProspectionOilLocationPage(getProspectionOilPosStr(blockX, blockZ))));
        }

        // set book data
        compound.setString("author", position);
        compound.setTag("pages", bookPages);
        setNBT(output, compound);

        // Mimic original behaviour
        return new GTScannerResult(
                ScannerHandlerLoader.SCAN_PROSPECTING_DATA_EUT,
                ScannerHandlerLoader.SCAN_PROSPECTING_DATA_DURATION,
                1,
                0,
                0,
                output);
    }
}
