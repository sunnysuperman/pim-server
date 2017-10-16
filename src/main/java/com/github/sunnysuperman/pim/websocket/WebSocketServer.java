package com.github.sunnysuperman.pim.websocket;

import java.util.ArrayList;
import java.util.List;

import com.github.sunnysuperman.pim.bootstrap.ChannelHandlerWrap;
import com.github.sunnysuperman.pim.bootstrap.NIOServer;
import com.github.sunnysuperman.pim.bootstrap.ServerConfig;
import com.github.sunnysuperman.pim.bootstrap.ServerHandlerInitializer;
import com.github.sunnysuperman.pim.bootstrap.ServerOptions;
import com.github.sunnysuperman.pim.protocol.IdleHandler;
import com.github.sunnysuperman.pim.protocol.LogChannelHandler;

import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class WebSocketServer extends NIOServer {

    public WebSocketServer(ServerOptions options, WebSocketHandlerFactory handlerFactory) {
        super(options, new WebSocketServerHandlerInitializer(handlerFactory));
    }

    private static class WebSocketServerHandlerInitializer implements ServerHandlerInitializer {
        WebSocketHandlerFactory handlerFactory;

        public WebSocketServerHandlerInitializer(WebSocketHandlerFactory handlerFactory) {
            super();
            this.handlerFactory = handlerFactory;
        }

        @Override
        public List<ChannelHandlerWrap> createHandlers(ServerConfig config) {
            List<ChannelHandlerWrap> handlers = new ArrayList<>(3);
            if (config.isChannelLog()) {
                handlers.add(new ChannelHandlerWrap("log", new LogChannelHandler()));
            }
            handlers.add(new ChannelHandlerWrap("decoder", new HttpRequestDecoder()));
            handlers.add(new ChannelHandlerWrap("encoder", new HttpResponseEncoder()));
            handlers.add(new ChannelHandlerWrap("idle",
                    new IdleHandler(config, WebSocketAwareClientPacketWriter.getInstance())));
            handlers.add(new ChannelHandlerWrap("handler", handlerFactory.createHandler(config)));
            return handlers;
        }

    }

}