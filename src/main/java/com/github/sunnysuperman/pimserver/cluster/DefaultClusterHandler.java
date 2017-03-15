package com.github.sunnysuperman.pimserver.cluster;

import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pimsdk.Packet;
import com.github.sunnysuperman.pimsdk.PacketType;
import com.github.sunnysuperman.pimsdk.packet.MessageAck;
import com.github.sunnysuperman.pimsdk.packet.PongPacket;
import com.github.sunnysuperman.pimserver.PacketWriter;
import com.github.sunnysuperman.pimserver.ServerConfig;
import com.github.sunnysuperman.pimserver.ServerHandler;
import com.github.sunnysuperman.pimserver.Session;
import com.github.sunnysuperman.pimserver.client.ClientConnectionManager;
import com.github.sunnysuperman.pimserver.client.ClientPacketRouter;
import com.github.sunnysuperman.pimserver.packet.ClientPacket;
import com.github.sunnysuperman.pimserver.packet.CommunicationPacket;
import com.github.sunnysuperman.pimserver.packet.ServerPacketType;

public abstract class DefaultClusterHandler extends ServerHandler {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultClusterHandler.class);
	protected ClientConnectionManager clientConnectionManager;
	protected ClientPacketRouter packetRouter;

	public DefaultClusterHandler(ServerConfig config, ClientConnectionManager clientConnectionManager,
			ClientPacketRouter packetRouter) {
		super(config);
		this.clientConnectionManager = clientConnectionManager;
		this.packetRouter = packetRouter;
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		if (!handleNewChannel(ctx)) {
			ctx.close();
		}
	}

	@Override
	public final boolean handlePacket(Packet packet, ChannelHandlerContext channel, Session session) {
		byte type = packet.getType();
		if (type == TYPE_COMMUNICATION) {
			try {
				CommunicationPacket cPacket = CommunicationPacket.decode(packet.getBody());
				if (cPacket.getSequenceId() != null) {
					MessageAck ack = new MessageAck(cPacket.getSequenceId(), null);
					PacketWriter.write(ack, channel.channel());
				}
				if (cPacket instanceof ClientPacket) {
					ClientPacket clientPacket = (ClientPacket) cPacket;
					Packet payload = clientPacket.getPacket();
					boolean routed = packetRouter.route(payload, clientPacket.getClientID(),
							ClientPacketRouter.ROUTE_LOCAL) != ClientPacketRouter.ROUTE_NONE;
					if (payload.getType() == PacketType.TYPE_DISCONNECT) {
						try {
							clientConnectionManager.removeAndCloseConnection(clientPacket.getClientID());
						} catch (Throwable t) {
							LOG.error(null, t);
						}
					} else {
						if (!routed) {
							try {
								boolean removed = clientConnectionManager
										.removeFromRouteTableIfAbsent(clientPacket.getClientID());
								if (removed) {
									LOG.warn("removeFromRouteTableIfAbsent: "
											+ clientPacket.getClientID().getUsername());
								}
							} catch (Throwable t) {
								LOG.error(null, t);
							}
						}
					}
					routeHandled(routed, clientPacket);
				}
			} catch (Throwable t) {
				LOG.error(null, t);
			}
			return true;
		}
		if (type == ServerPacketType.TYPE_PONG) {
			return true;
		}
		if (type == ServerPacketType.TYPE_PING) {
			PacketWriter.write(new PongPacket(), channel.channel());
			return true;
		}
		return false;
	}

	protected abstract void routeHandled(boolean routed, ClientPacket clientPacket);

	protected abstract boolean accept(ChannelHandlerContext ctx, SocketAddress remoteAddress) throws Exception;

	protected boolean handleNewChannel(ChannelHandlerContext ctx) {
		SocketAddress remoteAddress = (SocketAddress) ctx.channel().remoteAddress();

		if (remoteAddress == null) {
			LOG.warn(DefaultClusterHandler.class.getCanonicalName() + " - remoteAddress is nulll");
			return false;
		}

		boolean accepted = false;
		try {
			accepted = accept(ctx, remoteAddress);
		} catch (Exception e) {
			LOG.error("Failed to accept remote address", e);
		}

		return accepted;
	}

	@Override
	protected Session newSession() {
		return new Session();
	}
}
