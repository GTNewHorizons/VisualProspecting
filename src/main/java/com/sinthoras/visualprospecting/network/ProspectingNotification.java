package com.sinthoras.visualprospecting.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;
import com.sinthoras.visualprospecting.database.UndergroundFluidPosition;
import com.sinthoras.visualprospecting.utils.VPByteBufUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class ProspectingNotification implements IMessage {

    private static final List<UndergroundFluidPosition> emptyUndergroundFluids = new ArrayList<>(0);

    private List<OreVeinPosition> oreVeins;
    private List<UndergroundFluidPosition> undergroundFluids;

    public ProspectingNotification() {}

    public ProspectingNotification(OreVeinPosition oreVeinPosition) {
        oreVeins = Collections.singletonList(oreVeinPosition);
        undergroundFluids = emptyUndergroundFluids;
    }

    public ProspectingNotification(List<OreVeinPosition> oreVeins, List<UndergroundFluidPosition> undergroundFluids) {
        this.oreVeins = oreVeins;
        this.undergroundFluids = undergroundFluids;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        oreVeins = VPByteBufUtils.ReadOreVeinPositions(buf);
        undergroundFluids = VPByteBufUtils.ReadUndergroundFluidPositions(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        VPByteBufUtils.WriteOreVeinPositions(buf, oreVeins);
        VPByteBufUtils.WriteUndergroundFluidPositions(buf, undergroundFluids);
    }

    public static class Handler implements IMessageHandler<ProspectingNotification, IMessage> {

        @Override
        public IMessage onMessage(ProspectingNotification message, MessageContext ctx) {
            ClientCache.instance.putOreVeins(message.oreVeins);
            ClientCache.instance.putUndergroundFluids(message.undergroundFluids);
            return null;
        }
    }
}
