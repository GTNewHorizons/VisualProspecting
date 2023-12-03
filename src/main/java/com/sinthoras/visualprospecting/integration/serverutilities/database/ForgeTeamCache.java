package com.sinthoras.visualprospecting.integration.serverutilities.database;

import com.sinthoras.visualprospecting.Tags;
import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.database.DimensionCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.database.WorldCache;
import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.integration.serverutilities.network.ClientFullSyncReqMsg;
import com.sinthoras.visualprospecting.integration.serverutilities.task.TeamSyncTaskBatcher;
import com.sinthoras.visualprospecting.integration.serverutilities.task.TeamSyncToPlayerTask;
import com.sinthoras.visualprospecting.task.TaskManager;
import net.minecraft.entity.player.EntityPlayerMP;
import serverutils.lib.data.ForgeTeam;

import java.io.File;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class ForgeTeamCache extends WorldCache {

    private final ForgeTeam team;
    private final String uid;
    private final ChangeList changeList = new ChangeList();
    private final Map<UUID, Long> memberSyncMap = new HashMap<>();

    protected File changeListPath;
    protected File memberSyncPath;

    private boolean isLoaded = false;
    private boolean isDirty = false;


    public ForgeTeamCache(ForgeTeam team) {
        this.team = team;
        this.uid = team.getUIDCode();
    }

    protected File getStorageDirectory() {
        final File teamDir = com.sinthoras.visualprospecting.Utils.getSubDirectory(Tags.TEAMS_DIR);
        return new File(teamDir, uid);
    }

    public ForgeTeam getTeam() {
        return this.team;
    }

    public void putOreVeins(List<OreVeinPosition> oreVeins) {
        long timestamp = System.currentTimeMillis();
        List<OreVeinPosition> modified = new ArrayList<>();

        for (OreVeinPosition oreVein : oreVeins) {
            if (putOreVein(oreVein) == DimensionCache.UpdateResult.AlreadyKnown) continue;

            changeList.addOreVein(oreVein, timestamp);
            modified.add(oreVein);
            isDirty = true;
        }

        if (modified.isEmpty())
            return;

        TeamSyncTaskBatcher.instance.addOreVeins(team, modified);

        for (EntityPlayerMP member : team.getOnlineMembers()) {
            memberSyncMap.put(member.getPersistentID(), timestamp);
        }
    }

    public void putUndergroundFluids(List<UndergroundFluidPosition> undergroundFluids) {
        long timestamp = System.currentTimeMillis();
        List<UndergroundFluidPosition> modified = new ArrayList<>();

        for (UndergroundFluidPosition undergroundFluid : undergroundFluids) {
            if (putUndergroundFluids(undergroundFluid) == DimensionCache.UpdateResult.AlreadyKnown) continue;

            changeList.addUndergroundFluid(undergroundFluid, timestamp);
            modified.add(undergroundFluid);
            isDirty = true;
        }

        if (modified.isEmpty())
            return;

        TeamSyncTaskBatcher.instance.addUndergroundFluids(team, modified);

        for (EntityPlayerMP member : team.getOnlineMembers()) {
            memberSyncMap.put(member.getPersistentID(), timestamp);
        }
    }

    public void syncPlayer(EntityPlayerMP player) {
        long lastSyncTimestamp = memberSyncMap.computeIfAbsent(player.getPersistentID(), uuid -> 0L);

        if (lastSyncTimestamp == 0L)
            VP.network.sendTo(new ClientFullSyncReqMsg(), player);

        ChangeList.ChangesPair changes = changeList.getAllSince(lastSyncTimestamp);

        memberSyncMap.put(player.getPersistentID(), System.currentTimeMillis());

        if (changes.isEmpty())
            return;

        List<OreVeinPosition> oreVeins = changes.oreVeinList()
                .stream()
                .map(posId -> getOreVein(posId.dimId, posId.chunkX, posId.chunkZ))
                .filter(vein -> vein.veinType != VeinType.NO_VEIN)
                .collect(Collectors.toList());

        List<UndergroundFluidPosition> undergroundFluids = changes.undergroundFluidList()
                .stream()
                .map(posId -> getUndergroundFluid(posId.dimId, posId.chunkX, posId.chunkZ))
                .filter(UndergroundFluidPosition::isProspected)
                .collect(Collectors.toList());

        TaskManager.instance.addTask(new TeamSyncToPlayerTask(player, oreVeins, undergroundFluids));
    }

    public void removePlayer(EntityPlayerMP player) {
        memberSyncMap.remove(player.getPersistentID());
    }

    public void delete(String worldId) {
        reset();

        final File worldCacheDir = new File(getStorageDirectory(), worldId);
        Utils.deleteDirectoryRecursively(worldCacheDir);
    }

    @Override
    public void reset() {
        super.reset();
        changeList.clear();
        memberSyncMap.clear();
        isLoaded = false;
        isDirty = false;
    }

    @Override
    public void saveVeinCache() {
        super.saveVeinCache();

        if (!isDirty)
            return;

        if (changeListPath == null || memberSyncPath == null)
            return;

        final ByteBuffer changeListBytes = changeList.toBytes();
        Utils.writeToFile(changeListPath, changeListBytes);

        final ByteBuffer memberSyncBytes = saveMemberSyncMap();
        Utils.writeToFile(memberSyncPath, memberSyncBytes);

        isDirty = false;
    }

    @Override
    public boolean loadVeinCache(String worldId) {
        if (isLoaded)
            return true;
        isLoaded = true;

        super.loadVeinCache(worldId);

        changeListPath = new File(getStorageDirectory(), worldId + "/changelist");
        memberSyncPath = new File(getStorageDirectory(), worldId + "/members");

        ByteBuffer changeListBuf = Utils.readFileToBuffer(changeListPath);
        if (changeListBuf != null)
            changeList.fromBytes(changeListBuf);

        ByteBuffer memberSyncBuf = Utils.readFileToBuffer(memberSyncPath);
        if (memberSyncBuf != null)
            loadMemberSyncMap(memberSyncBuf);

        return true;
    }

    private ByteBuffer saveMemberSyncMap() {
        final int byteSize = Integer.BYTES + (memberSyncMap.size() * (Long.BYTES * 3));
        ByteBuffer buf = ByteBuffer.allocate(byteSize);

        buf.putInt(memberSyncMap.size());
        memberSyncMap.forEach((uuid, timestamp) -> {
            buf.putLong(uuid.getMostSignificantBits());
            buf.putLong(uuid.getLeastSignificantBits());
            buf.putLong(timestamp);
        });

        buf.flip();
        return buf;
    }

    private void loadMemberSyncMap(ByteBuffer buf) {
        try {
            final int count = buf.getInt();
            for (int i = 0; i < count; i++) {
                final long mostSigBits = buf.getLong();
                final long leastSigBits = buf.getLong();
                final long timestamp = buf.getLong();

                memberSyncMap.put(new UUID(mostSigBits, leastSigBits), timestamp);
            }
        } catch (BufferUnderflowException exception) {
            exception.printStackTrace();
        }
    }
}
