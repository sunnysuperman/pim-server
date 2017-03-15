package com.github.sunnysuperman.pimserver.client;

import io.netty.channel.ChannelHandlerContext;

import com.github.sunnysuperman.pimsdk.ClientID;
import com.github.sunnysuperman.pimserver.cluster.ClusterPacketRouter;
import com.github.sunnysuperman.pimserver.cluster.RouteTable;
import com.github.sunnysuperman.pimserver.packet.OfflinePacket;
import com.github.sunnysuperman.pimserver.packet.OnlinePacket;
import com.github.sunnysuperman.pimserver.region.RegionPacketRouter;

public interface ClientConnectionManager {

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
