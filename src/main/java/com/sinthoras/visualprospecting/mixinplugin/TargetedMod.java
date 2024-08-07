package com.sinthoras.visualprospecting.mixinplugin;

public enum TargetedMod {

    BARTWORKS("BartWorks", "com.github.bartimaeusnek.bartworks.ASM.BWCorePlugin", "bartworks"),
    GALACTICGREG("Galactic Greg", null, "galacticgreg"),
    GT5U("GregTech5u", null, "gregtech"), // Also matches GT6.
    VANILLA("Minecraft", null);

    /** The "name" in the @Mod annotation */
    public final String modName;
    /** Class that implements the IFMLLoadingPlugin interface */
    public final String coreModClass;
    /** The "modid" in the @Mod annotation */
    public final String modId;

    TargetedMod(String modName, String coreModClass) {
        this(modName, coreModClass, null);
    }

    TargetedMod(String modName, String coreModClass, String modId) {
        this.modName = modName;
        this.coreModClass = coreModClass;
        this.modId = modId;
    }

    @Override
    public String toString() {
        return "TargetedMod{modName='" + modName + "', coreModClass='" + coreModClass + "', modId='" + modId + "'}";
    }
}
