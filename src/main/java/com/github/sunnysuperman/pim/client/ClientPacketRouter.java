package com.github.sunnysuperman.pim.client;

import io.netty.channel.ChannelHandlerContext;

import com.github.sunnysuperman.pim.protocol.ClientID;
import com.github.sunnysuperman.pim.protocol.Packet;

public interface ClientPacketRouter {
    public static final int ROUTE_NONE = 0;
    public static final int ROUTE_LOCAL = (1 << 0);
    public static final int ROUTE_CLUSTER = (1 << 1);
    public static final int ROUTE_GLOBAL = (1 << 2);

    int route(Packet packet, ClientID clientID);

    int route(Packet packet, ClientID clientID, int routeType);

    boolean write(Packet packet, ChannelHandlerContext channel);

    int getCompressThreshold();

    void setCompressThreshold(int compressThreshold);

}
