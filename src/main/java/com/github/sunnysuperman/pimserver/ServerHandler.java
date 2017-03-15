package com.github.sunnysuperman.pimserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pimsdk.packet.PingPacket;
import com.github.sunnysuperman.pimserver.packet.ServerPacketType;

public abstract class ServerHandler extends PacketHandler implements ServerPacketType {
	private static final Logger LOG = LoggerFactory.getLogger(ServerHandler.class);
	protected static final PingPacket PING = new PingPacket();
	protected volatile byte state; // 0 - none, 1 - initialized, 2 - destroyed
	protected volatile ScheduledFuture<?> timer;
	protected volatile long lastReadTime;
	protected volatile boolean reading;
	protected NIOServer server;

	public ServerHandler(ServerConfig config) {
		super(config);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
			// channelActvie() event has been fired already, which means
			// this.channelActive() will
			// not be invoked. We have to initialize here instead.
			initialize(ctx);
		} else {
			// channelActive() event has not been fired yet.
			// this.channelActive() will be invoked
			// and initialization will occur there.
		}
		super.handlerAdded(ctx);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		destroy();
		super.handlerRemoved(ctx);
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		// Initialize early if channel is active already.
		if (ctx.channel().isActive()) {
			initialize(ctx);
		}
		super.channelRegistered(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// This method will be invoked only if this handler was added
		// before channelActive() event is fired. If a user adds this handler
		// after the channelActive() event, initialize() will be called by
		// beforeAdd().
		initialize(ctx);
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		destroy();
		super.channelInactive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		reading = true;
		super.channelRead(ctx, msg);
	}

	@Override
	public final void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		reading = false;
		lastReadTime = System.currentTimeMillis();
		super.channelReadComplete(ctx);
	}

	private void initialize(ChannelHandlerContext ctx) {
		// Avoid the case where destroy() is called before scheduling timeouts.
		// See: https://github.com/netty/netty/issues/143
		switch (state) {
		case 1:
		case 2:
			return;
		}

		state = 1;

		int readIdleTimeMills = ((ServerConfig) config).getReadIdleTimeMills();
		if (readIdleTimeMills > 0) {
			EventExecutor loop = ctx.executor();
			timer = loop.schedule(new ReaderIdleTimeoutTask(ctx), readIdleTimeMills, TimeUnit.MILLISECONDS);
		}
	}

	private void destroy() {
		state = 2;

		if (timer != null) {
			timer.cancel(false);
			timer = null;
		}
	}

	private final class ReaderIdleTimeoutTask implements Runnable {
		private final ChannelHandlerContext ctx;
		private boolean idle;

		private ReaderIdleTimeoutTask(ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void run() {
			try {
				if (!ctx.channel().isOpen()) {
					return;
				}
				long nextDelay = ((ServerConfig) config).getReadIdleTimeMills();
				if (!reading) {
					nextDelay -= (System.currentTimeMillis() - lastReadTime);
				}

				if (idle) {
					if (nextDelay <= 0) {
						if (config.isChannelLog()) {
							logChannel(ctx, "Read timeout");
						}
						ctx.close();
					} else {
						idle = false;
						timer = ctx.executor().schedule(this, nextDelay, TimeUnit.MILLISECONDS);
					}
				} else {
					if (nextDelay <= 0) {
						// Reader is idle - send ping and set a new timeout.
						if (config.isChannelLog()) {
							logChannel(ctx, "Read idle, send ping");
						}
						PacketWriter.write(PING, ctx.channel());
						idle = true;
						timer = ctx.executor().schedule(this, ((ServerConfig) config).getWaitPongTimeMills(),
								TimeUnit.MILLISECONDS);
					} else {
						// Read occurred before the timeout - set a new timeout
						// with shorter delay.
						timer = ctx.executor().schedule(this, nextDelay, TimeUnit.MILLISECONDS);
					}
				}
			} catch (Throwable t) {
				LOG.error("ReaderIdleTimeoutTask execute failed", t);
				ctx.close();
			}
		}
	}

	public NIOServer getServer() {
		return server;
	}

	public void setServer(NIOServer server) {
		this.server = server;
	}

}
