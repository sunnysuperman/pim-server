package com.github.sunnysuperman.pim.cluster;

import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pim.bootstrap.ServerConfig;
import com.github.sunnysuperman.pim.client.ClientConnectionManager;
import com.github.sunnysuperman.pim.client.ClientPacketRouter;
import com.github.sunnysuperman.pim.protocol.ClientPacket;
import com.github.sunnysuperman.pim.protocol.CommunicationPacket;
import com.github.sunnysuperman.pim.protocol.DefaultPacketWriter;
import com.github.sunnysuperman.pim.protocol.MessageAck;
import com.github.sunnysuperman.pim.protocol.Packet;
import com.github.sunnysuperman.pim.protocol.PacketHandler;
import com.github.sunnysuperman.pim.protocol.PacketType;
import com.github.sunnysuperman.pim.protocol.PongPacket;
import com.github.sunnysuperman.pim.protocol.ServerPacketType;

import io.netty.channel.ChannelHandlerContext;

public abstract class DefaultClusterPacketHandler extends PacketHandler {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultClusterPacketHandler.class);
	protected ClientConnectionManager clientConnectionManager;
	protected ClientPacketRouter packetRouter;

	public DefaultClusterPacketHandler(ServerConfig config, ClientConnectionManager clientConnectionManager,
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
	public final boolean handlePacket(ChannelHandlerContext channel, Packet packet) {
		byte type = packet.getType();
		if (type == ServerPacketType.TYPE_COMMUNICATION) {
			try {
				CommunicationPacket cPacket = CommunicationPacket.decode(packet.getBody());
				if (cPacket.getSequenceId() != null) {
					MessageAck ack = new MessageAck(cPacket.getSequenceId(), null);
					DefaultPacketWriter.getInstance().write(ack, channel.channel());
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
			DefaultPacketWriter.getInstance().write(new PongPacket(), channel.channel());
			return true;
		}
		return false;
	}

	protected abstract void routeHandled(boolean routed, ClientPacket clientPacket);

	protected abstract boolean accept(ChannelHandlerContext ctx, SocketAddress remoteAddress) throws Exception;

	protected boolean handleNewChannel(ChannelHandlerContext ctx) {
		SocketAddress remoteAddress = (SocketAddress) ctx.channel().remoteAddress();

		if (remoteAddress == null) {
			LOG.warn(DefaultClusterPacketHandler.class.getCanonicalName() + " - remoteAddress is nulll");
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
}
