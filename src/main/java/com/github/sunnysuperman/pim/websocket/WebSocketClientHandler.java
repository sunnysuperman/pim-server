package com.github.sunnysuperman.pim.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pim.bootstrap.ServerConfig;
import com.github.sunnysuperman.pim.client.DefaultClientPacketProcessor;
import com.github.sunnysuperman.pim.protocol.Packet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class WebSocketClientHandler extends WebSocketHandler {
	private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientHandler.class);
	protected DefaultClientPacketProcessor processor;

	public WebSocketClientHandler(ServerConfig config, DefaultClientPacketProcessor processor) {
		super(config);
		this.processor = processor;
	}

	@Override
	public final boolean handlePacket(ChannelHandlerContext channel, WebSocketFrame frame) {
		if (!(frame instanceof BinaryWebSocketFrame)) {
			LOG.warn("Bad frame");
			return false;
		}
		BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;
		Packet packet = WebSocketPacketCodec.decode(binaryFrame);
		return processor.handlePacket(channel, packet);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		processor.channelActive(ctx);
		super.channelActive(ctx);
	}

	@Override
	public final void channelInactive(ChannelHandlerContext ctx) throws Exception {
		processor.channelInactive(ctx);
		super.channelInactive(ctx);
	}

}
