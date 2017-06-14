package com.github.sunnysuperman.pim.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.pim.client.ClientAuthProvider.AuthResult;
import com.github.sunnysuperman.pim.protocol.ClientID;
import com.github.sunnysuperman.pim.protocol.ConnectAckPacket;
import com.github.sunnysuperman.pim.protocol.Packet;
import com.github.sunnysuperman.pim.protocol.PacketType;
import com.github.sunnysuperman.pim.protocol.PongPacket;
import com.github.sunnysuperman.pim.protocol.ServerKey;
import com.github.sunnysuperman.pim.util.PimUtil;

public abstract class DefaultClientPacketProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultClientPacketProcessor.class);
	private final ClientConnectionManager connectionManager;
	private final ClientPacketRouter packetRouter;
	private final ClientAuthProvider authProvider;

	public DefaultClientPacketProcessor(ClientConnectionManager connectionManager,
			ClientPacketRouter packetRouter, ClientAuthProvider authProvider) {
		this.connectionManager = connectionManager;
		this.packetRouter = packetRouter;
		this.authProvider = authProvider;
	}

	public ClientConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public ClientPacketRouter getPacketRouter() {
		return packetRouter;
	}

	public ClientAuthProvider getAuthProvider() {
		return authProvider;
	}

	public final boolean handlePacket(ChannelHandlerContext channel, Packet packet) {
		Attribute<ClientSession> sessionAttr = channel.channel().attr(ServerKey.KEY_SESSION);
		ClientSession session = sessionAttr.get();
		if (session == null) {
			LOG.error("No session on handlePacket");
			return false;
		}
		byte type = packet.getType();
		if (type == PacketType.TYPE_CONNECT) {
			AuthResult result = authProvider.auth(channel, packet.getBody());
			Map<String, Object> res = new HashMap<String, Object>(2);
			res.put("err", result.errorCode);
			if (result.compressEnabled) {
				// set compress threshold to client
				res.put("cThreshold", packetRouter.getCompressThreshold());
				// set channel compress enabled
				session.setCompressEnabled(true);
			}
			String s = JSONUtil.toJSONString(res);
			ConnectAckPacket ack = new ConnectAckPacket(PimUtil.wrapBytes(s));

			ClientSession clientSession = (ClientSession) session;
			if (result.clientID == null) {
				packetRouter.write(ack, channel);
			} else {
				// add to connection manager
				session.setClientID(result.clientID);
				boolean added = false;
				try {
					added = connectionManager.addConnection(result.clientID, channel);
				} catch (Throwable t) {
					LOG.error("Failed to add connection for " + result.clientID, t);
				}
				if (!added) {
					return false;
				}
				// response
				packetRouter.write(ack, channel);
			}
			return clientLoginCompleted(channel, clientSession, result);
		}
		if (type == PacketType.TYPE_PONG) {
			return true;
		}
		if (type == PacketType.TYPE_PING) {
			packetRouter.write(new PongPacket(), channel);
			return true;
		}
		return clientMessageReceived(channel, session, packet);
	}

	public final void channelActive(ChannelHandlerContext ctx) throws Exception {
		// create session
		Attribute<ClientSession> sessionAttr = ctx.channel().attr(ServerKey.KEY_SESSION);
		sessionAttr.set(newSession());
	}

	public final void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Attribute<ClientSession> sessionAttr = ctx.channel().attr(ServerKey.KEY_SESSION);
		ClientSession session = (ClientSession) sessionAttr.get();
		if (session == null) {
			return;
		}
		ClientID clientID = session.getClientID();
		if (clientID != null) {
			connectionManager.removeConnection(clientID, ctx);
		}
		clientDisconnected(ctx, session);
	}

	protected abstract boolean clientLoginCompleted(ChannelHandlerContext channel, ClientSession session,
			AuthResult result);

	protected abstract void clientDisconnected(ChannelHandlerContext channel, ClientSession session);

	protected abstract boolean clientMessageReceived(ChannelHandlerContext channel, ClientSession session,
			Packet packet);

	protected abstract ClientSession newSession();
}
