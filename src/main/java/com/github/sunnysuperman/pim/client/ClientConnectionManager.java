package com.github.sunnysuperman.pim.client;

import io.netty.channel.ChannelHandlerContext;

import com.github.sunnysuperman.pim.cluster.ClusterPacketRouter;
import com.github.sunnysuperman.pim.cluster.RouteTable;
import com.github.sunnysuperman.pim.protocol.ClientID;
import com.github.sunnysuperman.pim.protocol.OfflinePacket;
import com.github.sunnysuperman.pim.protocol.OnlinePacket;
import com.github.sunnysuperman.pim.region.RegionPacketRouter;

public interface ClientConnectionManager {

	String getMyServerId();

	boolean isResourceEnabled();

	RouteTable getRouteTable();

	ClusterPacketRouter getClusterPacketRouter();

	RegionPacketRouter getRegionPacketRouter();

	boolean isRegionEnabled();

	boolean addConnection(ClientID clientID, ChannelHandlerContext channel) throws Exception;

	boolean removeConnection(ClientID clientID, ChannelHandlerContext channel) throws Exception;

	boolean removeAndCloseConnection(ClientID clientID) throws Exception;

	boolean removeFromRouteTableIfAbsent(ClientID clientID) throws Exception;

	ChannelHandlerContext findConnection(ClientID clientID) throws Exception;

	void onRemoteLogin(OnlinePacket onlinePacket) throws Exception;

	void onRemoteLogout(OfflinePacket offlinePacket) throws Exception;

	int countConnections();

	void stop();
}
