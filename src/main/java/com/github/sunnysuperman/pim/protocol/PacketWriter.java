package com.github.sunnysuperman.pim.protocol;

import io.netty.channel.Channel;

public interface PacketWriter {
	public static final int NO_COMPRESS_THRESHOLD = -1;

	boolean write(Packet packet, Channel channel, int compressThreshold);

	boolean write(Packet packet, Channel channel);

}
