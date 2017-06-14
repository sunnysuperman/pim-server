package com.github.sunnysuperman.pim.protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;

import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.commons.config.Config;
import com.github.sunnysuperman.commons.utils.StringUtil;
import com.github.sunnysuperman.pim.protocol.PacketHandler.PacketHandlerConfig;

public abstract class ConnectionBuilder {

	public static class ConnectionBuilderConfig extends PacketHandlerConfig {
		public ConnectionBuilderConfig(Config config, String name) {
			super(config, name);
		}

		protected volatile int handleThreads;
		protected volatile int connectTimeoutMills;
		protected volatile SSLContext sslContext;
		protected volatile int readBufferSize;
		protected volatile int writeBufferSize;

		@Override
		protected void initValues() {
			if (handleThreads <= 0) {
				handleThreads = 1;
			}
			if (connectTimeoutMills <= 0) {
				connectTimeoutMills = 20000;
			}
			super.initValues();
		}

		@Override
		protected boolean validate(String key, Object value) {
			if (key.equals("handleThreads")) {
				return ((Integer) value) > 0;
			}
			if (key.equals("connectTimeoutMills")) {
				return ((Integer) value) > 0;
			}
			if (key.equals("writeTimeoutMills")) {
				return ((Integer) value) > 0;
			}
			return super.validate(key, value);
		}

		public int getHandleThreads() {
			return handleThreads;
		}

		public void setHandleThreads(int handleThreads) {
			this.handleThreads = handleThreads;
		}

		public int getConnectTimeoutMills() {
			return connectTimeoutMills;
		}

		public void setConnectTimeoutMills(int connectTimeoutMills) {
			this.connectTimeoutMills = connectTimeoutMills;
		}

		public SSLContext getSslContext() {
			return sslContext;
		}

		public void setSslContext(SSLContext sslContext) {
			this.sslContext = sslContext;
		}

		public int getReadBufferSize() {
			return readBufferSize;
		}

		public void setReadBufferSize(int readBufferSize) {
			this.readBufferSize = readBufferSize;
		}

		public int getWriteBufferSize() {
			return writeBufferSize;
		}

		public void setWriteBufferSize(int writeBufferSize) {
			this.writeBufferSize = writeBufferSize;
		}

	}

	protected static final Logger LOG = LoggerFactory.getLogger(ConnectionBuilder.class);
	protected final EventLoopGroup eventLoop;
	protected final ConnectionBuilderConfig config;

	public ConnectionBuilder(ConnectionBuilderConfig config) {
		super();
		this.eventLoop = new NioEventLoopGroup(config.getHandleThreads());
		this.config = config;
	}

	protected abstract PacketHandler newPacketHandler(Object context);

	protected abstract Connection setupConnection(Channel channel, Object context);

	protected final Connection build(final String hosts, final int port, final Object context) {
		Connection connection = null;
		List<String> hostList = StringUtil.split(hosts, ",");
		if (hostList.size() > 1) {
			Collections.shuffle(hostList);
		}
		for (String host : hostList) {
			host = host.trim();
			if (host.isEmpty()) {
				continue;
			}
			Channel channel = null;
			try {
				Bootstrap b = new Bootstrap();
				b.group(eventLoop).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true)
						.option(ChannelOption.TCP_NODELAY, true);
				if (config.getReadBufferSize() > 0) {
					b.option(ChannelOption.SO_RCVBUF, config.getReadBufferSize());
				}
				if (config.getWriteBufferSize() > 0) {
					b.option(ChannelOption.SO_SNDBUF, config.getWriteBufferSize());
				}
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMills());
				b.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						if (config.sslContext != null) {
							SSLEngine sslEngine = config.sslContext.createSSLEngine();
							sslEngine.setUseClientMode(true);
							pipeline.addLast(new SslHandler(sslEngine));
						}
						pipeline.addLast(newPacketHandler(context));
					}
				});
				ChannelFuture f = b.connect(host, port);
				channel = f.channel();
				LOG.warn("Build connection start: " + channel.toString());
				f.sync();
				connection = setupConnection(channel, context);
			} catch (Throwable t) {
				LOG.error("Failed to build connection to " + host + ":" + port, t);
			}
			if (connection == null) {
				try {
					if (channel != null) {
						channel.close();
					}
				} catch (Throwable e) {
					// ignore
				}
			} else {
				// Get a connection, return it
				LOG.warn("Build connection to " + host + ":" + port + " successfully, "
						+ connection.getChannel().toString());
				break;
			}
		}
		return connection;
	}
}
