package com.github.sunnysuperman.pimserver;

import io.netty.channel.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pimsdk.Packet;
import com.github.sunnysuperman.pimserver.packet.PacketOutput;

public class PacketWriter {
	private static final Logger LOG = LoggerFactory.getLogger(PacketWriter.class);
	public static final int NO_COMPRESS_THRESHOLD = -1;

	public static boolean write(Packet packet, Channel channel, int compressThreshold) {
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

	public static boolean write(Packet packet, Channel channel) {
		return write(packet, channel, NO_COMPRESS_THRESHOLD);
	}
}
