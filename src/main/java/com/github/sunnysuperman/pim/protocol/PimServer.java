package com.github.sunnysuperman.pim.protocol;

import java.util.ArrayList;
import java.util.List;

import com.github.sunnysuperman.pim.bootstrap.ChannelHandlerWrap;
import com.github.sunnysuperman.pim.bootstrap.NIOServer;
import com.github.sunnysuperman.pim.bootstrap.ServerConfig;
import com.github.sunnysuperman.pim.bootstrap.ServerHandlerInitializer;
import com.github.sunnysuperman.pim.bootstrap.ServerOptions;

public class PimServer extends NIOServer {

	public PimServer(ServerOptions options, PimServerHandlerFactory handlerFactory) {
		super(options, new PimServerHandlerInitializer(handlerFactory));
	}

	private static class PimServerHandlerInitializer implements ServerHandlerInitializer {
		PimServerHandlerFactory handlerFactory;

		public PimServerHandlerInitializer(PimServerHandlerFactory handlerFactory) {
			super();
			this.handlerFactory = handlerFactory;
		}

		@Override
		public List<ChannelHandlerWrap> createHandlers(ServerConfig config) {
			List<ChannelHandlerWrap> handlers = new ArrayList<>(3);
			if (config.isChannelLog()) {
				handlers.add(new ChannelHandlerWrap("log", new LogChannelHandler()));
			}
			handlers.add(new ChannelHandlerWrap("idle", new IdleHandler(config, DefaultPacketWriter.getInstance())));
			handlers.add(new ChannelHandlerWrap("handler", handlerFactory.createHandler(config)));
			return handlers;
		}

	}

}