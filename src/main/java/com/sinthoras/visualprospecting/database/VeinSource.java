package com.sinthoras.visualprospecting.database;

/**
 * Introduced for v3 of the server/client cache format.<br/>
 * Store value as {@code byte} for tinier footprint.<br/>
 * <br/>
 * <b>APPEND ONLY</b>, reordering or inserting values will silently corrupt existing data.
 */
public enum VeinSource {

    UNKNOWN,
    GENERATED,
    RESCAN,
    API;
    // Add new values here not anywhere else

    private static final VeinSource[] VALUES = values();

    public static VeinSource fromByte(byte ordinal) {
        if (ordinal >= 0 && ordinal < VALUES.length) {
            return VALUES[ordinal];
        }
        return UNKNOWN;
    }
}
