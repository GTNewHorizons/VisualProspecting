package com.sinthoras.visualprospecting.database;

/**
 * Added for v3 of the server/client cache format. Stored as a {@code byte} for tinier footprint.
 * <p>
 * <b>Never change or reuse the {@code id} of an existing constant</b>, it would silently corrupt existing caches.
 */
public enum VeinSource {

    // id = byte on disk to match to this enum | trustLevel = trust hierarchy for overwriting
    UNKNOWN(0, 0),
    RESCAN(2, 1),
    API(3, 2),
    GENERATED(1, 3);

    private final int id;
    private final int trustLevel;

    VeinSource(int id, int trustLevel) {
        this.id = id;
        this.trustLevel = trustLevel;
    }

    public boolean canOverwrite(VeinSource existing) {
        return this.trustLevel >= existing.trustLevel;
    }

    public byte toByte() {
        return (byte) id;
    }

    private static final VeinSource[] BY_ID;
    static {
        int maxId = 0;
        for (VeinSource source : values()) {
            maxId = Math.max(maxId, source.id);
        }
        BY_ID = new VeinSource[maxId + 1];
        for (VeinSource source : values()) {
            BY_ID[source.id] = source;
        }
    }

    public static VeinSource fromByte(byte id) {
        return ((id >= 0) && (id < BY_ID.length) && (BY_ID[id] != null)) ? BY_ID[id] : UNKNOWN;
    }
}
