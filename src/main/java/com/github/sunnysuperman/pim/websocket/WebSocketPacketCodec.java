package com.github.sunnysuperman.pim.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import com.github.sunnysuperman.pim.protocol.DefaultPacket;
import com.github.sunnysuperman.pim.protocol.Packet;

public class WebSocketPacketCodec {

	public static BinaryWebSocketFrame encode(Packet packet, Channel channel) {
		ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
		byte[] body = packet.getBody();
		int bodyLen = body == null ? 0 : body.length;
		ByteBuf buf = allocator.directBuffer(1 + bodyLen);
		buf.writeByte(packet.getType());
		if (bodyLen > 0) {
			buf.writeBytes(body, 0, body.length);
		}
		return new BinaryWebSocketFrame(buf);
	}

	public static Packet decode(BinaryWebSocketFrame frame) {
		ByteBuf buf = frame.content();
		int len = buf.readableBytes();
		byte type = buf.readByte();
		byte[] body = new byte[len - 1];
		buf.readBytes(body);
		return new DefaultPacket(type, body);
	}

}
