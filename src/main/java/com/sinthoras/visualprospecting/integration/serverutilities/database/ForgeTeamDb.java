package com.sinthoras.visualprospecting.integration.serverutilities.database;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.data.ForgeTeam;

public class ForgeTeamDb {

    public static final ForgeTeamDb instance = new ForgeTeamDb();

    private final Map<String, ForgeTeamCache> teamCacheMap = new HashMap<>();

    public ForgeTeamCache get(ForgeTeam team) {
        return teamCacheMap.computeIfAbsent(team.getUIDCode(), (uid) -> new ForgeTeamCache(team));
    }

    public @Nullable ForgeTeamCache getByPlayer(EntityPlayerMP player) {
        return teamCacheMap.values().stream()
                .filter(
                        value -> value.getTeam().getOnlineMembers().stream()
                                .anyMatch(teamPlayer -> teamPlayer.getPersistentID().equals(player.getPersistentID())))
                .findFirst().orElse(null);
    }

    public void syncPlayer(EntityPlayerMP player) {
        ForgeTeamCache teamCache = getByPlayer(player);
        if (teamCache == null) return;

        teamCache.syncPlayer(player);
    }

    public void save() {
        for (ForgeTeamCache teamCache : teamCacheMap.values()) {
            teamCache.saveVeinCache();
        }
    }

    public void reset() {
        for (ForgeTeamCache teamCache : teamCacheMap.values()) {
            teamCache.reset();
        }
        teamCacheMap.clear();
    }

    public void delete(String uid, String worldId) {
        if (!teamCacheMap.containsKey(uid)) return;

        ForgeTeamCache teamCache = teamCacheMap.get(uid);
        teamCache.delete(worldId);
        teamCacheMap.remove(uid);
    }

    public void delete(ForgeTeam team, String worldId) {
        delete(team.getUIDCode(), worldId);
    }
}
