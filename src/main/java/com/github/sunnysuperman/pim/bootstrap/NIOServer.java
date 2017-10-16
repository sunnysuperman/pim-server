package com.github.sunnysuperman.pim.bootstrap;

import java.util.ArrayList;
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

    private static class ServerHandlerWrap extends ChannelInitializer<SocketChannel> {
        private ServerConfig config;
        private ServerHandlerInitializer handlerInitializer;

        public ServerHandlerWrap(ServerConfig config, ServerHandlerInitializer handlerInitializer) {
            super();
            this.config = config;
            this.handlerInitializer = handlerInitializer;
        }

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
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
            List<ChannelHandlerWrap> handlers = handlerInitializer.createHandlers(config);
            for (ChannelHandlerWrap handler : handlers) {
                pipeline.addLast(handler.getName(), handler.getHandler());
            }
        }
    }

    private ServerOptions options;
    private ServerHandlerInitializer handlerInitializer;
    private List<Channel> serverChannels;

    public NIOServer(ServerOptions options, ServerHandlerInitializer handlerInitializer) {
        super();
        this.options = options;
        this.handlerInitializer = handlerInitializer;
    }

    public void start() throws Exception {
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
        if (options.getListens().isEmpty()) {
            throw new RuntimeException("No server listen configured");
        }
        serverChannels = new ArrayList<Channel>(options.getListens().size());
        for (ServerConfig config : options.getListens()) {
            int port = config.getPort();
            int backlog = config.getBacklog();
            int readBufferSize = config.getReadBufferSize();
            int writeBufferSize = config.getWriteBufferSize();
            ServerHandlerWrap handlerWrap = new ServerHandlerWrap(config, handlerInitializer);
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, backlog).option(ChannelOption.SO_REUSEADDR, true)
                    .handler(new ChannelInboundHandlerAdapter()).childHandler(handlerWrap);
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
    }

    public void stop() {
        for (Channel serverChannel : serverChannels) {
            serverChannel.close();
        }
    }

    public void waitUntilStopped() {
        for (Channel serverChannel : serverChannels) {
            try {
                serverChannel.closeFuture().await();
            } catch (Exception e) {
                LOG.error(null, e);
            }
        }
    }

}
