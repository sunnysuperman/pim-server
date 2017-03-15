package com.github.sunnysuperman.pimserver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;

public class NIOServer {
	protected static final Logger LOG = LoggerFactory.getLogger(NIOServer.class);

	public static interface ServerListener {
		void onStart(NIOServer server);

		void onStop(NIOServer server, boolean error);
	}

	public static class ServerOptions {
		private int bossThreads;
		private int workerThreads;
		private List<ServerConfig> listens;

		public int getBossThreads() {
			return bossThreads;
		}

		public void setBossThreads(int bossThreads) {
			this.bossThreads = bossThreads;
		}

		public int getWorkerThreads() {
			return workerThreads;
		}

		public void setWorkerThreads(int workerThreads) {
			this.workerThreads = workerThreads;
		}

		public List<ServerConfig> getListens() {
			return listens;
		}

		public void setListens(List<ServerConfig> listens) {
			this.listens = listens;
		}

	}

	public static class ServerHandlerWrap extends ChannelInitializer<SocketChannel> {
		private ServerHandlerFactory handleFactory;
		private ServerConfig config;
		private NIOServer server;

		private ServerHandlerWrap(NIOServer server, ServerHandlerFactory handleFactory, ServerConfig config) {
			super();
			this.server = server;
			this.handleFactory = handleFactory;
			this.config = config;
			// for init
			handleFactory.createHandler(config);
		}

		@Override
		public void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			ServerHandler handler = handleFactory.createHandler(config);
			handler.setServer(server);
			SSLContext sslContext = config.getSslContext();
			if (sslContext != null) {
				SSLEngine engine = sslContext.createSSLEngine();
				engine.setUseClientMode(false);
				if (config.getSslMode() <= 1) {
					engine.setNeedClientAuth(true);
				} else if (config.getSslMode() == 2) {
					engine.setWantClientAuth(true);
				} else {
					engine.setWantClientAuth(false);
				}
				pipeline.addLast("ssl", new SslHandler(engine));
			}
			pipeline.addLast("handler", handler);
		}
	}

	private List<Channel> serverChannels;
	private LinkedList<ServerListener> listeners = new LinkedList<ServerListener>();
	private ServerOptions options;
	private ServerHandlerFactory handlerFactory;

	public NIOServer(ServerOptions options, ServerHandlerFactory handlerFactory) {
		this.options = options;
		this.handlerFactory = handlerFactory;
		serverChannels = new ArrayList<Channel>(1);
	}

	public void addListener(ServerListener listener) {
		listeners.add(listener);
	}

	public void start(boolean runInSeparateThread) {
		if (runInSeparateThread) {
			new ServerBootstrapThread().start();
		} else {
			doStart();
		}
	}

	public class ServerBootstrapThread extends Thread {

		public void run() {
			doStart();
		}
	}

	public void doStart() {
		boolean error = false;
		int bossThreads = options.getBossThreads();
		if (bossThreads <= 0) {
			bossThreads = 1;
		}
		int workerThreads = options.getWorkerThreads();
		if (workerThreads <= 0) {
			workerThreads = Runtime.getRuntime().availableProcessors() * 2;
		}
		EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreads);
		EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads);
		try {
			for (ServerConfig config : options.getListens()) {
				int port = config.getPort();
				int backlog = config.getBacklog();
				int readBufferSize = config.getReadBufferSize();
				int writeBufferSize = config.getWriteBufferSize();
				ServerHandlerWrap handlerWrap = new ServerHandlerWrap(this, handlerFactory, config);
				ServerBootstrap b = new ServerBootstrap();
				b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.option(ChannelOption.SO_BACKLOG, backlog).option(ChannelOption.TCP_NODELAY, true)
						.option(ChannelOption.SO_REUSEADDR, true).handler(new ChannelInboundHandlerAdapter())
						.childHandler(handlerWrap);
				if (readBufferSize > 0) {
					b.option(ChannelOption.SO_RCVBUF, readBufferSize);
				}
				if (writeBufferSize > 0) {
					b.option(ChannelOption.SO_SNDBUF, writeBufferSize);
				}
				ChannelFuture f = b.bind(port).sync();
				Channel serverChannel = f.channel();
				serverChannels.add(serverChannel);
				LOG.warn("Listen port " + port + " ok.");
			}

			options.setListens(null);
			options = null;

			if (serverChannels.isEmpty()) {
				throw new RuntimeException("Listen none!!!");
			}
			for (ServerListener listener : listeners) {
				listener.onStart(this);
			}
			for (Channel serverChannel : serverChannels) {
				serverChannel.closeFuture().sync();
			}
		} catch (Exception e) {
			LOG.error("Server halted", e);
			error = true;
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			for (ServerListener listener : listeners) {
				listener.onStop(this, error);
			}
		}
	}

	public void stop() {
		for (Channel serverChannel : serverChannels) {
			serverChannel.close();
		}
	}

}
