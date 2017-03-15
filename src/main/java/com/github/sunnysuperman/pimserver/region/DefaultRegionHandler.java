package com.github.sunnysuperman.pimserver.region;

import io.netty.channel.ChannelHandlerContext;

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
import com.github.sunnysuperman.pimserver.packet.OfflinePacket;
import com.github.sunnysuperman.pimserver.packet.OnlinePacket;

public class DefaultRegionHandler extends ServerHandler {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultRegionHandler.class);
	protected ClientConnectionManager clientConnectionManager;
	protected ClientPacketRouter packetRouter;

	public DefaultRegionHandler(ServerConfig config, ClientConnectionManager clientConnectionManager,
			ClientPacketRouter packetRouter) {
		super(config);
		this.clientConnectionManager = clientConnectionManager;
		this.packetRouter = packetRouter;
	}

	@Override
	protected Session newSession() {
		return new Session();
	}

	@Override
	protected boolean handlePacket(Packet packet, ChannelHandlerContext channel, Session session) {
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
					int routed = packetRouter.route(payload, clientPacket.getClientID(),
							ClientPacketRouter.ROUTE_CLUSTER);
					boolean routeLocal = (routed & ClientPacketRouter.ROUTE_LOCAL) != 0;
					if (routeLocal && payload.getType() == PacketType.TYPE_DISCONNECT) {
						clientConnectionManager.removeAndCloseConnection(clientPacket.getClientID());
					}
				} else if (cPacket instanceof OnlinePacket) {
					OnlinePacket onlinePacket = (OnlinePacket) cPacket;
					clientConnectionManager.onRemoteLogin(onlinePacket);
				} else if (cPacket instanceof OfflinePacket) {
					OfflinePacket offlinePacket = (OfflinePacket) cPacket;
					clientConnectionManager.onRemoteLogout(offlinePacket);
				} else {
					LOG.error("No handler for packet: " + cPacket.getClass());
				}
			} catch (Throwable t) {
				LOG.error(null, t);
			}
			return true;
		}
		if (type == TYPE_PONG) {
			return true;
		}
		if (type == TYPE_PING) {
			PacketWriter.write(new PongPacket(), channel.channel());
			return true;
		}
		return false;
	}
}
