package com.sinthoras.visualprospecting.database;

/**
 * Introduced for v3 of the server/client cache format.<br/>
 * Store value as {@code byte} for tinier footprint.<br/>
 * <br/>
 * <b>APPEND ONLY</b>, reordering or inserting values will silently corrupt existing data.
 */
public enum VeinSource {

    UNKNOWN(0),
    GENERATED(3),
    RESCAN(1),
    API(2);
    // Add new values here not anywhere else. Arg is trust hierarchy for overwriting.
    // Trust order: GENERATED > API > RESCAN > UNKNOWN

    private static final VeinSource[] VALUES = values();
    private final int trustLevel;

    VeinSource(int trustLevel) {
        this.trustLevel = trustLevel;
    }

    public boolean canOverwrite(VeinSource existing) {
        return this.trustLevel >= existing.trustLevel;
    }

    public static VeinSource fromByte(byte ordinal) {
        if (ordinal >= 0 && ordinal < VALUES.length) {
            return VALUES[ordinal];
        }
        return UNKNOWN;
    }
}
