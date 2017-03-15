package com.github.sunnysuperman.pimserver.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.pimsdk.Packet;
import com.github.sunnysuperman.pimsdk.PacketType;
import com.github.sunnysuperman.pimsdk.packet.ConnectAckPacket;
import com.github.sunnysuperman.pimsdk.packet.PongPacket;
import com.github.sunnysuperman.pimsdk.util.PimUtil;
import com.github.sunnysuperman.pimserver.ServerConfig;
import com.github.sunnysuperman.pimserver.ServerHandler;
import com.github.sunnysuperman.pimserver.ServerKey;
import com.github.sunnysuperman.pimserver.Session;
import com.github.sunnysuperman.pimserver.client.ClientAuthProvider.AuthResult;

public abstract class DefaultClientHandler extends ServerHandler {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultClientHandler.class);
	protected final ClientConnectionManager clientConnectionManager;
	protected final ClientPacketRouter packetRouter;
	protected final ClientAuthProvider authProvider;

	public DefaultClientHandler(ServerConfig config, ClientConnectionManager clientConnectionManager,
			ClientPacketRouter packetRouter, ClientAuthProvider authProvider) {
		super(config);
		this.clientConnectionManager = clientConnectionManager;
		this.packetRouter = packetRouter;
		this.authProvider = authProvider;
	}

	public final ClientConnectionManager getClientConnectionManager() {
		return clientConnectionManager;
	}

	public final ClientPacketRouter getPacketRouter() {
		return packetRouter;
	}

	public final ClientAuthProvider getAuthProvider() {
		return authProvider;
	}

	@Override
	public final boolean handlePacket(Packet packet, ChannelHandlerContext channel, Session session) {
		byte type = packet.getType();
		if (type == PacketType.TYPE_CONNECT) {
			AuthResult result = authProvider.auth(channel, packet);
			Map<String, Object> res = new HashMap<String, Object>(2);
			res.put("err", result.errorCode);
			if (result.compressEnabled) {
				// set compress threshold to client
				res.put("cThreshold", packetRouter.getCompressThreshold());
				// set channel compress enabled
				channel.channel().attr(ServerKey.KEY_COMPRESS).set(Boolean.TRUE);
			}
			String s = JSONUtil.toJSONString(res);
			ConnectAckPacket ack = new ConnectAckPacket(PimUtil.wrapBytes(s));

			ClientSession clientSession = (ClientSession) session;
			if (result.clientID == null) {
				packetRouter.write(ack, channel);
			} else {
				// add to connection manager
				clientSession.clientID = result.clientID;
				boolean added = false;
				try {
					added = clientConnectionManager.addConnection(result.clientID, channel);
				} catch (Throwable t) {
					LOG.error("Failed to add connection for " + result.clientID, t);
				}
				if (!added) {
					return false;
				}
				// response
				packetRouter.write(ack, channel);
			}
			return onLoginCompleted(channel, clientSession, result);
		}
		if (type == PacketType.TYPE_PONG) {
			return true;
		}
		if (type == PacketType.TYPE_PING) {
			packetRouter.write(new PongPacket(), channel);
			return true;
		}
		return handleMessage(packet, channel, (ClientSession) session);
	}

	@Override
	protected ClientSession newSession() {
		return new ClientSession();
	}

	@Override
	public final void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		Attribute<Session> sessionAttr = ctx.channel().attr(ServerKey.KEY_SESSION);
		ClientSession session = (ClientSession) sessionAttr.get();
		if (session == null) {
			return;
		}
		if (session != null && session.clientID != null) {
			clientConnectionManager.removeConnection(session.clientID, ctx);
		}
		clientDisconnected(ctx, session);
	}

	protected abstract boolean onLoginCompleted(ChannelHandlerContext channel, ClientSession session, AuthResult result);

	protected abstract void clientDisconnected(ChannelHandlerContext channel, ClientSession session);

	protected abstract boolean handleMessage(Packet packet, ChannelHandlerContext channel, ClientSession session);

}
