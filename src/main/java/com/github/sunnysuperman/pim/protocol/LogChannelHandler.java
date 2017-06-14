package com.github.sunnysuperman.pim.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

public class LogChannelHandler extends ChannelInboundHandlerAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(LogChannelHandler.class);

	protected void logChannel(ChannelHandlerContext ctx, String message) {
		String chStr = ctx.channel().toString();
		String msg = new StringBuilder(chStr.length() + message.length() + 1).append(chStr).append(' ').append(message)
				.toString();
		LOG.info(msg);
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		logChannel(ctx, "channelRegistered");
		super.channelRegistered(ctx);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		logChannel(ctx, "channelUnregistered");
		super.channelUnregistered(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logChannel(ctx, "Active");
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logChannel(ctx, "Inactive");
		super.channelInactive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logChannel(ctx, "channelRead");
		super.channelRead(ctx, msg);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logChannel(ctx, "channelReadComplete");
		super.channelReadComplete(ctx);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof SslHandshakeCompletionEvent) {
			SslHandshakeCompletionEvent event = (SslHandshakeCompletionEvent) evt;
			logChannel(ctx, "SSL handle shake result: " + event.isSuccess());
		} else {
			logChannel(ctx, "UserEventTriggered: " + evt.getClass().getCanonicalName());
		}
		super.userEventTriggered(ctx, evt);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logChannel(ctx, "exceptionCaught");
	}

}
