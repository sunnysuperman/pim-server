package com.github.sunnysuperman.pimserver.client;

import io.netty.channel.ChannelHandlerContext;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pimsdk.ClientID;
import com.github.sunnysuperman.pimsdk.packet.DisconnectPacket;
import com.github.sunnysuperman.pimserver.PacketWriter;
import com.github.sunnysuperman.pimserver.cluster.ClusterPacketRouter;
import com.github.sunnysuperman.pimserver.cluster.RouteResult;
import com.github.sunnysuperman.pimserver.cluster.RouteTable;
import com.github.sunnysuperman.pimserver.packet.ClientPacket;
import com.github.sunnysuperman.pimserver.packet.CommunicationPacketTypes;
import com.github.sunnysuperman.pimserver.packet.OfflinePacket;
import com.github.sunnysuperman.pimserver.packet.OnlinePacket;
import com.github.sunnysuperman.pimserver.region.RegionPacketRouter;
import com.github.sunnysuperman.pimserver.util.Utils;

public class DefaultClientConnectionManager implements ClientConnectionManager {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultClientConnectionManager.class);
	private final byte[] LOCK = new byte[0];
	private static final DisconnectPacket DISCONNECT_PACKET = (DisconnectPacket) new DisconnectPacket(
			DisconnectPacket.ERROR_DUPLICATE).marshall();
	protected final Map<String, ChannelHandlerContext> connections = new ConcurrentHashMap<String, ChannelHandlerContext>();
	protected final ClusterPacketRouter clusterPacketRouter;
	protected final RegionPacketRouter regionPacketRouter;
	protected final boolean multiRegionLoginNotificationEnabled;
	protected final RouteTable routeTable;
	protected final String myServerId;
	protected final boolean resourceEnabled;
	protected final RouteResult myRouteResult;
	protected volatile boolean stopped;

	static {
		CommunicationPacketTypes.init();
	}

	public DefaultClientConnectionManager(boolean resourceEnabled, RouteTable routeTable,
			ClusterPacketRouter clusterPacketRouter, RegionPacketRouter regionPacketRouter,
			boolean multiRegionLoginNotificationEnabled) {
		super();
		this.resourceEnabled = resourceEnabled;
		this.routeTable = Utils.notNull(routeTable);
		this.clusterPacketRouter = Utils.notNull(clusterPacketRouter);
		this.myServerId = Utils.notNull(clusterPacketRouter.getMyServerId());
		this.regionPacketRouter = regionPacketRouter;
		this.multiRegionLoginNotificationEnabled = multiRegionLoginNotificationEnabled;
		this.myRouteResult = resourceEnabled ? null : new RouteResult(
				regionPacketRouter != null ? regionPacketRouter.getMyRegionId() : null, myServerId, null);
	}

	@Override
	public final boolean isResourceEnabled() {
		return resourceEnabled;
	}

	@Override
	public final RouteTable getRouteTable() {
		return routeTable;
	}

	@Override
	public final ClusterPacketRouter getClusterPacketRouter() {
		return clusterPacketRouter;
	}

	@Override
	public final RegionPacketRouter getRegionPacketRouter() {
		return regionPacketRouter;
	}

	@Override
	public final boolean isRegionEnabled() {
		return regionPacketRouter != null;
	}

	private RouteResult getLocalRouteResult(ClientID clientID) {
		if (myRouteResult != null) {
			return myRouteResult;
		} else {
			return new RouteResult(regionPacketRouter != null ? regionPacketRouter.getMyRegionId() : null, myServerId,
					clientID.getResource());
		}
	}

	@Override
	public boolean addConnection(ClientID clientID, ChannelHandlerContext channel) throws Exception {
		if (stopped) {
			return false;
		}
		if (resourceEnabled) {
			if (clientID.getResource() == null) {
				throw new RuntimeException("resource should not be null of " + clientID);
			}
		} else {
			if (clientID.getResource() != null) {
				throw new RuntimeException("resource should be null of " + clientID);
			}
		}
		String clientKey = makeClientKey(clientID);
		RouteResult myRoute = getLocalRouteResult(clientID);
		ChannelHandlerContext oldConnection = null;
		// update route table
		synchronized (LOCK) {
			if (stopped) {
				return false;
			}
			oldConnection = connections.put(clientKey, channel);
			try {
				routeTable.add(clientID.getUsername(), myRoute);
			} catch (Throwable t) {
				LOG.error("Failed to add route", t);
				connections.remove(clientKey);
				// re-put old connection
				if (oldConnection != null) {
					connections.put(clientKey, oldConnection);
				}
				return false;
			}
		}
		// close local duplicate connection
		if (oldConnection != null) {
			removeLocalhostDuplicateConnection(clientKey, oldConnection);
		}
		// close cluster duplicate connection
		removeClusterDuplicateConnection(clientID, false);
		// notify to other region server
		if (isMultiRegionLoginNotificationEnabled()) {
			try {
				regionPacketRouter.routeToAll(new OnlinePacket(clientID, getLocalRouteResult(clientID)), 0);
			} catch (Throwable t) {
				LOG.error(null, t);
			}
		}
		return true;
	}

	private boolean isMultiRegionLoginNotificationEnabled() {
		return multiRegionLoginNotificationEnabled && regionPacketRouter != null
				&& regionPacketRouter.getRegionCount() > 1;
	}

	private void removeLocalhostDuplicateConnection(String clientKey, ChannelHandlerContext connection) {
		try {
			if (LOG.isInfoEnabled()) {
				LOG.info("Close local duplicate connection: " + clientKey + ", " + Utils.printChannel(connection));
			}
			PacketWriter.write(DISCONNECT_PACKET, connection.channel());
			connection.close();
		} catch (Throwable t) {
			LOG.error(null, t);
		}
	}

	private void removeClusterDuplicateConnection(ClientID clientID, boolean alsoRemoveLocalhost) {
		// close local and cluster duplicate connection
		try {
			Set<RouteResult> routes = routeTable.get(clientID.getUsername());
			if (routes != null) {
				final String myRegionId = regionPacketRouter == null ? null : regionPacketRouter.getMyRegionId();
				for (RouteResult route : routes) {
					if (!route.matchResourceAndRegion(clientID, myRegionId)) {
						continue;
					}
					boolean local = route.getServer().equals(myServerId);
					if (local) {
						if (alsoRemoveLocalhost) {
							if (LOG.isInfoEnabled()) {
								LOG.info("Close local duplicate connection: " + clientID.getUsername() + ", " + route);
							}
							String clientKey = makeClientKey(clientID);
							ChannelHandlerContext connection = connections.get(clientKey);
							if (connection != null) {
								removeLocalhostDuplicateConnection(clientKey, connection);
							}
						}
					} else {
						if (LOG.isInfoEnabled()) {
							LOG.info("Close cluster duplicate connection: " + clientID.getUsername() + ", " + route);
						}
						clusterPacketRouter.route(route.getServer(), new ClientPacket(DISCONNECT_PACKET, clientID));
					}
				}
			}
		} catch (Throwable t) {
			LOG.error(null, t);
		}
	}

	@Override
	public void onRemoteLogin(OnlinePacket onlinePacket) throws Exception {
		ClientID clientID = onlinePacket.getClientID();
		// update route table
		try {
			routeTable.add(clientID.getUsername(), onlinePacket.getRoute());
		} catch (Throwable t) {
			LOG.error(null, t);
		}
		removeClusterDuplicateConnection(clientID, true);
	}

	@Override
	public void onRemoteLogout(OfflinePacket offlinePacket) throws Exception {
		try {
			routeTable.remove(offlinePacket.getClientID().getUsername(), offlinePacket.getRoute());
		} catch (Throwable t) {
			LOG.error(null, t);
		}
	}

	private void offlineNotify(ClientID clientID) {
		try {
			if (isMultiRegionLoginNotificationEnabled()) {
				regionPacketRouter.routeToAll(new OfflinePacket(clientID, getLocalRouteResult(clientID)), 0);
			}
		} catch (Throwable t) {
			LOG.error(null, t);
		}
	}

	@Override
	public boolean removeConnection(ClientID clientID, ChannelHandlerContext channel) throws Exception {
		String clientKey = makeClientKey(clientID);
		boolean removed = false;
		synchronized (LOCK) {
			ChannelHandlerContext oldChannel = connections.get(clientKey);
			if (oldChannel == channel) {
				remove(clientID);
				removed = true;
			}
		}
		if (removed) {
			offlineNotify(clientID);
		}
		return false;
	}

	@Override
	public ChannelHandlerContext findConnection(ClientID clientID) throws Exception {
		String clientKey = makeClientKey(clientID);
		return connections.get(clientKey);
	}

	@Override
	public boolean removeAndCloseConnection(ClientID clientID) throws Exception {
		ChannelHandlerContext connection = null;
		synchronized (LOCK) {
			connection = remove(clientID);
		}
		if (connection != null) {
			connection.close();
			offlineNotify(clientID);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeFromRouteTableIfAbsent(ClientID clientID) throws Exception {
		String clientKey = makeClientKey(clientID);
		if (connections.containsKey(clientKey)) {
			// 预判断，不加锁，提高效率
			return false;
		}
		synchronized (LOCK) {
			if (connections.containsKey(clientKey)) {
				return false;
			}
			removeFromRouteTable(clientID);
		}
		offlineNotify(clientID);
		return true;
	}

	@Override
	public int countConnections() {
		return connections.size();
	}

	@Override
	public void stop() {
		stopped = true;
		LOG.warn("The server will refuse any new connections");
		ChannelHandlerContext connection = null;
		while (true) {
			synchronized (LOCK) {
				if (connections.isEmpty()) {
					break;
				}
				for (Iterator<Entry<String, ChannelHandlerContext>> iter = connections.entrySet().iterator(); iter
						.hasNext();) {
					Entry<String, ChannelHandlerContext> entry = iter.next();
					ClientID clientID = ClientID.wrap(entry.getKey());
					connection = entry.getValue();
					removeFromRouteTable(clientID);
					iter.remove();
					break;
				}
			}
			if (connection != null) {
				connection.close();
				connection = null;
			}
		}
	}

	private ChannelHandlerContext remove(ClientID clientID) {
		String clientKey = makeClientKey(clientID);
		ChannelHandlerContext connection = connections.remove(clientKey);
		removeFromRouteTable(clientID);
		return connection;
	}

	private void removeFromRouteTable(ClientID clientID) {
		try {
			routeTable.remove(clientID.getUsername(), getLocalRouteResult(clientID));
		} catch (Throwable t) {
			LOG.error("Failed to remove route: " + clientID.toString(), t);
		}
	}

	private String makeClientKey(ClientID clientID) {
		return resourceEnabled ? clientID.toString() : clientID.getUsername();
	}

}
