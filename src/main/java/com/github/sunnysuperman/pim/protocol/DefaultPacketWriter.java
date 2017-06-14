package com.github.sunnysuperman.pim.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

public class DefaultPacketWriter implements PacketWriter {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultPacketWriter.class);
	private static final DefaultPacketWriter INSTANCE = new DefaultPacketWriter();

	public static final DefaultPacketWriter getInstance() {
		return INSTANCE;
	}

	private DefaultPacketWriter() {

	}

	@Override
	public boolean write(Packet packet, Channel channel, int compressThreshold) {
		try {
			PacketOutput output = new PacketOutput(packet.getType());
			byte[] body = packet.getBody();
			if (body != null) {
				output.appendBody(body);
			}
			channel.writeAndFlush(output.serialize(compressThreshold));
			return true;
		} catch (Throwable t) {
			LOG.error(null, t);
			return false;
		}
	}

	@Override
	public boolean write(Packet packet, Channel channel) {
		return write(packet, channel, NO_COMPRESS_THRESHOLD);
	}

}
