package com.github.sunnysuperman.pim.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pim.protocol.DefaultPacketWriter;
import com.github.sunnysuperman.pim.protocol.Packet;
import com.github.sunnysuperman.pim.protocol.PacketWriter;
import com.github.sunnysuperman.pim.protocol.ServerKey;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class WebSocketAwareClientPacketWriter implements PacketWriter {
	private static final Logger LOG = LoggerFactory.getLogger(WebSocketAwareClientPacketWriter.class);
	private static final WebSocketAwareClientPacketWriter INSTANCE = new WebSocketAwareClientPacketWriter();

	public static final WebSocketAwareClientPacketWriter getInstance() {
		return INSTANCE;
	}

	private WebSocketAwareClientPacketWriter() {

	}

	@Override
	public boolean write(Packet packet, Channel channel, int compressThreshold) {
		Boolean websocket = channel.attr(ServerKey.KEY_WEBSOCKET).get();
		if (websocket != null && websocket.booleanValue()) {
			try {
				BinaryWebSocketFrame frame = WebSocketPacketCodec.encode(packet, channel);
				channel.writeAndFlush(frame);
				return true;
			} catch (Throwable t) {
				LOG.error(null, t);
				return false;
			}
		} else {
			return DefaultPacketWriter.getInstance().write(packet, channel, compressThreshold);
		}
	}

	@Override
	public boolean write(Packet packet, Channel channel) {
		return write(packet, channel, PacketWriter.NO_COMPRESS_THRESHOLD);
	}

}
