package com.sinthoras.visualprospecting;

import net.minecraft.client.resources.I18n;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fluids.Fluid;

import com.sinthoras.visualprospecting.database.veintypes.VeinType;

// Backup translations for server side lookups only
public class ServerTranslations {

    @SuppressWarnings("deprecation")
    public static String getEnglishLocalization(Fluid fluid) {
        if (MinecraftServer.getServer().isSinglePlayer()) {
            return fluid.getLocalizedName();
        }

        return switch (fluid.getUnlocalizedName()) {
            case "fluid.gas_natural_gas" -> "Natural Gas";
            case "fluid.liquid_light_oil" -> "Light Oil";
            case "fluid.liquid_medium_oil" -> "Raw Oil";
            case "fluid.liquid_heavy_oil" -> "Heavy Oil";
            case "fluid.oil" -> "Oil";
            case "fluid.drillingfluid" -> "Drilling Fluid";
            case "fluid.helium-3" -> "Helium-3";
            case "fluid.saltwater" -> "Saltwater";
            case "fluid.molten.iron" -> "Molten Iron";
            case "fluid.molten.lead" -> "Molten Lead";
            case "fluid.sulfuricacid" -> "Sulfuric Acid";
            case "fluid.carbondioxide" -> "Carbondioxide";
            case "fluid.chlorobenzene" -> "Chlorobenzene";
            case "fluid.liquid_extra_heavy_oil" -> "Extra Heavy Oil";
            case "fluid.ic2distilledwater" -> "Distilled Water";
            case "fluid.oxygen" -> "Oxygen";
            case "fluid.liquidair" -> "Liquid Air";
            case "fluid.methane" -> "Methane";
            case "fluid.ethane" -> "Ethane";
            case "fluid.liquid_hydricsulfur" -> "Liquid Hydric Sulfur";
            case "fluid.carbonmonoxide" -> "Carbonmonoxide";
            case "fluid.nitrogen" -> "Nitrogen";
            case "fluid.ethylene" -> "Ethylene";
            case "fluid.deuterium" -> "Deuterium";
            case "fluid.fluorine" -> "Fluorine";
            case "fluid.hydrofluoricacid_gt5u" -> "Hydrofluoric Acid";
            case "fluid.molten.copper" -> "Molten Copper";
            case "fluid.unknowwater" -> "Unknowwater";
            case "fluid.molten.tin" -> "Molten Tin";
            case "fluid.hydrogen" -> "Hydrogen";
            case "fluid.lava" -> "Lava";
            default -> fluid.getUnlocalizedName();
        };
    }

    /*
     * Copy the relevant content into a file 'data.dat' and run the following python script to generate the switch-case:
     * with open("data.dat") as f: lines = f.readlines() for line in lines: if line is not "\n": key, value =
     * line.split("=") value = value[:-1] print("case \"" + key + "\":") print("    return \"" + value + "\";")
     */
    public static String getEnglishLocalization(VeinType veinType) {
        if (MinecraftServer.getServer().isSinglePlayer()) {
            return I18n.format(veinType.name);
        }

        return switch (veinType.name) {
            case "ore.mix.naquadah" -> "Naquadah";
            case "ore.mix.lignite" -> "Lignite";
            case "ore.mix.coal" -> "Coal";
            case "ore.mix.magnetite" -> "Magnetite";
            case "ore.mix.gold" -> "Gold";
            case "ore.mix.iron" -> "Iron";
            case "ore.mix.cassiterite" -> "Cassiterite";
            case "ore.mix.tetrahedrite" -> "Tetrahedrite";
            case "ore.mix.netherquartz" -> "Nether Quartz";
            case "ore.mix.sulfur" -> "Sulfur";
            case "ore.mix.copper" -> "Copper";
            case "ore.mix.bauxite" -> "Bauxite";
            case "ore.mix.salts" -> "Salts";
            case "ore.mix.redstone" -> "Redstone";
            case "ore.mix.soapstone" -> "Soapstone";
            case "ore.mix.nickel" -> "Nickel";
            case "ore.mix.platinum" -> "Platinum";
            case "ore.mix.pitchblende" -> "Pitchblende";
            case "ore.mix.monazite" -> "Monazite";
            case "ore.mix.molybdenum" -> "Molybdenum";
            case "ore.mix.tungstate" -> "Tungstate";
            case "ore.mix.sapphire" -> "Sapphire";
            case "ore.mix.manganese" -> "Manganese";
            case "ore.mix.quartz" -> "Quartz";
            case "ore.mix.diamond" -> "Diamond";
            case "ore.mix.olivine" -> "Olivine";
            case "ore.mix.apatite" -> "Apatite";
            case "ore.mix.galena" -> "Galena";
            case "ore.mix.lapis" -> "Lapis";
            case "ore.mix.beryllium" -> "Beryllium";
            case "ore.mix.uranium" -> "Uranium";
            case "ore.mix.oilsand" -> "Oilsands";
            case "ore.mix.neutronium" -> "Neutronium";
            case "ore.mix.aquaignis" -> "Aqua and Ignis";
            case "ore.mix.terraaer" -> "Terra and Aer";
            case "ore.mix.perditioordo" -> "Perdito and Ordo";
            case "ore.mix.coppertin" -> "Vermiculite";
            case "ore.mix.titaniumchrome" -> "Ilmenite";
            case "ore.mix.mineralsand" -> "Mineralsand";
            case "ore.mix.garnettin" -> "Garnettin";
            case "ore.mix.kaolinitezeolite" -> "Kaolinite";
            case "ore.mix.mica" -> "Mica";
            case "ore.mix.dolomite" -> "Dolomite";
            case "ore.mix.platinumchrome" -> "Palladium";
            case "ore.mix.iridiummytryl" -> "Iridium";
            case "ore.mix.osmium" -> "Osmium";
            case "ore.mix.saltpeterelectrotine" -> "Electrotine";
            case "ore.mix.desh" -> "Desh";
            case "ore.mix.draconium" -> "Draconium";
            case "ore.mix.quantium" -> "Quantum";
            case "ore.mix.callistoice" -> "Callisto Ice";
            case "ore.mix.mytryl" -> "Mithril";
            case "ore.mix.ledox" -> "Ledox";
            case "ore.mix.oriharukon" -> "Oriharukon";
            case "ore.mix.blackplutonium" -> "Black Plutonium";
            case "ore.mix.infusedgold" -> "Infused Gold";
            case "ore.mix.niobium" -> "Niobium";
            case "ore.mix.tungstenirons" -> "Tungsten";
            case "ore.mix.uraniumgtnh" -> "Thorium";
            case "ore.mix.vanadiumgold" -> "Vanadium";
            case "ore.mix.netherstar" -> "NetherStar";
            case "ore.mix.garnet" -> "Garnet";
            case "ore.mix.rareearth" -> "Rare Earths";
            case "ore.mix.richnuclear" -> "Plutonium";
            case "ore.mix.heavypentele" -> "Arsenic";
            case "ore.mix.europa" -> "Magnesite";
            case "ore.mix.europacore" -> "Chrome";
            case "ore.mix.secondlanthanid" -> "Samarium";
            case "ore.mix.quartzspace" -> "Quartz";
            case "ore.mix.rutile" -> "Rutile";
            case "ore.mix.tfgalena" -> "Cryolite";
            case "ore.mix.luvtantalite" -> "Pyrolusit";
            case "ore.mix.ross128.Thorianit" -> "Thorianit";
            case "ore.mix.ross128.carbon" -> "Graphite";
            case "ore.mix.ross128.bismuth" -> "Bismuth";
            case "ore.mix.ross128.TurmalinAlkali" -> "Olenit";
            case "ore.mix.ross128.Roquesit" -> "Roquesit";
            case "ore.mix.ross128.Tungstate" -> "Scheelite";
            case "ore.mix.ross128.CopperSulfits" -> "Djurleit";
            case "ore.mix.ross128.Forsterit" -> "Forsterit";
            case "ore.mix.ross128.Hedenbergit" -> "Hedenbergit";
            case "ore.mix.ross128.RedZircon" -> "Red Zircon";
            case "ore.mix.ross128ba.tib" -> "Tiberium";
            case "ore.mix.ross128ba.Tungstate" -> "Scheelite";
            case "ore.mix.ross128ba.bart" -> "BArTiMaEuSNeK";
            case "ore.mix.ross128ba.TurmalinAlkali" -> "Olenit";
            case "ore.mix.ross128ba.Amethyst" -> "Amethyst";
            case "ore.mix.ross128ba.CopperSulfits" -> "Djurleit";
            case "ore.mix.ross128ba.RedZircon" -> "Red Zircon";
            case "ore.mix.ross128ba.Fluorspar" -> "Fluorspa";
            default -> veinType.name;
        };
    }
}
