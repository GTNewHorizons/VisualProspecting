package com.sinthoras.visualprospecting.integration.serverutilities.database;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ChangeList<T> {

    private final NavigableMap<Long, List<T>> changes = new TreeMap<>(Comparator.naturalOrder());

    public void add(List<T> items, long timestamp) {
        changes.put(timestamp, items);
    }

    public void add(List<T> items) {
        final long timestamp = System.currentTimeMillis();
        this.add(items, timestamp);
    }

    /**
     * @param timestamp time in milliseconds to start from
     * @return A List of all changes since the provided timestamp to the most recent change.
     */
    public List<T> getAllSince(long timestamp) {
        final long latest = changes.lastKey();
        return changes.subMap(timestamp, true, latest, true)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .toList();
    }
}
