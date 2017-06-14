package com.github.sunnysuperman.pim.websocket;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pim.bootstrap.ServerConfig;
import com.github.sunnysuperman.pim.protocol.ServerKey;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public abstract class WebSocketHandler extends SimpleChannelInboundHandler<Object> {
	protected static final Logger LOG = LoggerFactory.getLogger(WebSocketHandler.class);
	protected ServerConfig config;

	public WebSocketHandler(ServerConfig config) {
		super();
		this.config = config;
	}

	// 这个方法：
	// 1.完成websocket前的http握手
	// 2.屏蔽掉非websocket握手请求
	private void processHttpRequest(ChannelHandlerContext context, HttpRequest request) {
		// 屏蔽掉非websocket握手请求
		// 只接受http GET和headers['Upgrade']为'websocket'的http请求
		if (!HttpMethod.GET.equals(request.method())
				|| !"websocket".equalsIgnoreCase(request.headers().get("Upgrade"))) {
			onBadRequest(context, request, HttpResponseStatus.BAD_REQUEST);
			return;
		}
		Channel channel = context.channel();
		String websocketURL = (config.getSslContext() != null ? "wss://" : "ws://")
				+ request.headers().get(HttpHeaderNames.HOST);
		WebSocketServerHandshakerFactory wsShakerFactory = new WebSocketServerHandshakerFactory(websocketURL, null,
				false);
		WebSocketServerHandshaker wsShakerHandler = wsShakerFactory.newHandshaker(request);
		if (wsShakerHandler == null) {
			// 无法处理的websocket版本
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
		} else {
			// 向客户端发送websocket握手,完成握手
			// 客户端收到的状态是101 sitching protocol
			wsShakerHandler.handshake(channel, request);
		}
	}

	private void onBadRequest(ChannelHandlerContext context, HttpRequest request, HttpResponseStatus status) {
		DefaultHttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
		context.writeAndFlush(resp);
		context.close();
	}

	protected void processWebsocketRequest(ChannelHandlerContext context, WebSocketFrame request) {
		if (request instanceof PingWebSocketFrame) {
			context.writeAndFlush(new PongWebSocketFrame());
		} else if (request instanceof PongWebSocketFrame) {
			// nope
		} else if (request instanceof CloseWebSocketFrame) {
			context.close();
		} else {
			boolean processed = handlePacket(context, request);
			if (!processed) {
				context.close();
			}
		}
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		// 虽然是websocket，但在建立websocket连接前，先进行http握手,所以，这时也要处理http请求
		// 在http握手完成后，才是websocket下的通信
		if (msg instanceof HttpRequest) {
			processHttpRequest(ctx, (HttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			processWebsocketRequest(ctx, (WebSocketFrame) msg);
		} else {
			// 未处理的请求类型
			ctx.close();
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.channel().attr(ServerKey.KEY_WEBSOCKET).set(Boolean.TRUE);
		super.channelActive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof IOException) {
			return;
		}
		LOG.error(null, cause);
	}

	protected abstract boolean handlePacket(ChannelHandlerContext context, WebSocketFrame packet);
}
