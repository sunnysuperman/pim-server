package com.github.sunnysuperman.pim.client;

import io.netty.channel.ChannelHandlerContext;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.commons.utils.CollectionUtil;
import com.github.sunnysuperman.pim.cluster.ClusterPacketRouter;
import com.github.sunnysuperman.pim.cluster.RouteResult;
import com.github.sunnysuperman.pim.cluster.RouteTable;
import com.github.sunnysuperman.pim.protocol.ClientID;
import com.github.sunnysuperman.pim.protocol.ClientPacket;
import com.github.sunnysuperman.pim.protocol.Packet;
import com.github.sunnysuperman.pim.protocol.PacketWriter;
import com.github.sunnysuperman.pim.protocol.ServerKey;
import com.github.sunnysuperman.pim.region.RegionPacketRouter;
import com.github.sunnysuperman.pim.util.Utils;

public class DefaultClientPacketRouter implements ClientPacketRouter {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultClientPacketRouter.class);
	private final PacketWriter clientPacketWriter;
	private final ClientConnectionManager clientConnetionManager;
	private final ClusterPacketRouter clusterPacketRouter;
	private final RegionPacketRouter regionPacketRouter;
	private final RouteTable routeTable;
	private final boolean resourceEnabled;
	private volatile int compressThreshold;

	public DefaultClientPacketRouter(PacketWriter clientPacketWriter,
			ClientConnectionManager clientConnetionManager, int compressThreshold) {
		this.clientPacketWriter = Utils.notNull(clientPacketWriter);
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
	public final boolean write(Packet packet, ChannelHandlerContext channel) {
		byte[] body = packet.getBody();
		int realCompressThreshold = PacketWriter.NO_COMPRESS_THRESHOLD;
		if (compressThreshold > 0 && body != null && body.length >= compressThreshold) {
			ClientSession session = channel.channel().attr(ServerKey.KEY_SESSION).get();
			if (session != null && session.isCompressEnabled()) {
				realCompressThreshold = compressThreshold;
			}
		}
		return clientPacketWriter.write(packet, channel.channel(), realCompressThreshold);
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
			if (routeType == ROUTE_LOCAL) {
				// local only
				return routed;
			}
			if (routed != ROUTE_NONE && !multiClient) {
				// Already routed
				return routed;
			}
		}
		final Set<RouteResult> routes;
		try {
			routes = routeTable.get(clientID.getUsername());
		} catch (Exception e) {
			LOG.error("Failed to get route of " + clientID.getUsername(), e);
			return routed;
		}
		// route to this region if one or more connection build
		if (CollectionUtil.isNotEmpty(routes)) {
			try {
				final String myServerId = clientConnetionManager.getMyServerId();
				final String myRegionId = regionPacketRouter == null ? null : regionPacketRouter.getMyRegionId();
				for (RouteResult route : routes) {
					if (!route.matchResourceAndRegion(clientID, myRegionId)) {
						continue;
					}
					ClientID target = multiClient ? ClientID.wrap(clientID.getUsername(), route.getResource())
							: clientID;
					boolean local = route.getServer().equals(myServerId);
					if (local) {
						ChannelHandlerContext connection = clientConnetionManager.findConnection(target);
						if (connection != null && write(packet, connection)) {
							routed |= ROUTE_LOCAL;
						}
						// TODO removeFromRouteTableIfAbsent
					} else {
						// TODO should be really routed
						if (clusterPacketRouter.route(route.getServer(), new ClientPacket(packet, target))) {
							routed |= ROUTE_CLUSTER;
						}
					}
				}
			} catch (Exception e) {
				LOG.error(null, e);
			}
		}
		if (routed != ROUTE_NONE && !multiClient) {
			// Already routed
			return routed;
		}
		// Not need to route global or multi region is disabled
		if (routeType != ROUTE_GLOBAL || regionPacketRouter == null || regionPacketRouter.getRegionCount() <= 1) {
			return routed;
		}
		// TODO only route to target region
		if (regionPacketRouter.routeToAll(new ClientPacket(packet, clientID), 0)) {
			routed |= ROUTE_GLOBAL;
		}
		return routed;
	}

}
