package com.github.sunnysuperman.pimserver.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pimsdk.ClientID;
import com.github.sunnysuperman.pimsdk.Packet;
import com.github.sunnysuperman.pimserver.PacketWriter;
import com.github.sunnysuperman.pimserver.ServerKey;
import com.github.sunnysuperman.pimserver.cluster.ClusterPacketRouter;
import com.github.sunnysuperman.pimserver.cluster.RouteResult;
import com.github.sunnysuperman.pimserver.cluster.RouteTable;
import com.github.sunnysuperman.pimserver.packet.ClientPacket;
import com.github.sunnysuperman.pimserver.region.RegionPacketRouter;
import com.github.sunnysuperman.pimserver.util.Utils;

public class DefaultClientPacketRouter implements ClientPacketRouter {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultClientPacketRouter.class);
	private final ClientConnectionManager clientConnetionManager;
	private final ClusterPacketRouter clusterPacketRouter;
	private final RegionPacketRouter regionPacketRouter;
	private final RouteTable routeTable;
	private final boolean resourceEnabled;
	private volatile int compressThreshold;

	public DefaultClientPacketRouter(ClientConnectionManager clientConnetionManager, int compressThreshold) {
		this.clientConnetionManager = Utils.notNull(clientConnetionManager);
		this.clusterPacketRouter = clientConnetionManager.getClusterPacketRouter();
		this.regionPacketRouter = clientConnetionManager.getRegionPacketRouter();
		this.routeTable = clientConnetionManager.getRouteTable();
		this.compressThreshold = compressThreshold;
		this.resourceEnabled = clientConnetionManager.isResourceEnabled();
	}

	public ClientConnectionManager getClientConnetionManager() {
		return clientConnetionManager;
	}

	@Override
	public int getCompressThreshold() {
		return compressThreshold;
	}

	@Override
	public void setCompressThreshold(int compressThreshold) {
		this.compressThreshold = compressThreshold;
	}

	@Override
	public boolean write(Packet packet, ChannelHandlerContext channel) {
		byte[] body = packet.getBody();
		if (compressThreshold > 0 && body != null && body.length >= compressThreshold) {
			Attribute<Boolean> compressAttr = channel.channel().attr(ServerKey.KEY_COMPRESS);
			Boolean compressEnabled = compressAttr.get();
			if (compressEnabled != null && compressEnabled.booleanValue()) {
				return PacketWriter.write(packet, channel.channel(), compressThreshold);
			}
		}
		return PacketWriter.write(packet, channel.channel(), PacketWriter.NO_COMPRESS_THRESHOLD);
	}

	@Override
	public int route(Packet packet, ClientID clientID) {
		return route(packet, clientID, ROUTE_GLOBAL);
	}

	@Override
	public int route(final Packet packet, final ClientID clientID, final int routeType) {
		final String targetResource = clientID.getResource();
		final boolean multiClient = resourceEnabled && targetResource == null;
		int routed = ROUTE_NONE;
		if (routeType == ROUTE_LOCAL || !multiClient) {
			try {
				ChannelHandlerContext connection = clientConnetionManager.findConnection(clientID);
				if (connection != null && write(packet, connection)) {
					routed |= ROUTE_LOCAL;
				}
			} catch (Exception e) {
				LOG.error(null, e);
			}
		}
		if (routeType == ROUTE_LOCAL) {
			// local only
			return routed;
		}
		if (routed != ROUTE_NONE && !multiClient) {
			// Already routed
			return routed;
		}
		final Set<RouteResult> routes;
		try {
			routes = routeTable.get(clientID.getUsername());
		} catch (Exception e) {
			LOG.error("Failed to get route of " + clientID.getUsername(), e);
			return routed;
		}
		if (routes == null || routes.isEmpty()) {
			return routed;
		}
		try {
			final String myServerId = clusterPacketRouter.getMyServerId();
			final String myRegionId = regionPacketRouter.getMyRegionId();
			for (RouteResult route : routes) {
				if (!route.matchResourceAndRegion(clientID, myRegionId)) {
					continue;
				}
				ClientID target = multiClient ? ClientID.wrap(clientID.getUsername(), route.getResource()) : clientID;
				boolean local = route.getServer().equals(myServerId);
				if (local) {
					ChannelHandlerContext connection = clientConnetionManager.findConnection(target);
					if (connection != null && write(packet, connection)) {
						routed |= ROUTE_LOCAL;
					}
				} else {
					if (clusterPacketRouter.route(route.getServer(), new ClientPacket(packet, target))) {
						routed |= ROUTE_CLUSTER;
					}
				}
			}
		} catch (Exception e) {
			LOG.error(null, e);
		}
		if (routed != ROUTE_NONE && !multiClient) {
			// Already routed
			return routed;
		}
		if (routeType != ROUTE_GLOBAL || regionPacketRouter == null || regionPacketRouter.getRegions().size() <= 1) {
			return routed;
		}
		// TODO only route to target region
		if (regionPacketRouter.routeToAll(new ClientPacket(packet, clientID), 0)) {
			routed |= ROUTE_GLOBAL;
		}
		return routed;
	}

}