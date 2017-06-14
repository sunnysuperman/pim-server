package com.github.sunnysuperman.pim.bootstrap;

import io.netty.channel.ChannelHandler;

public class ChannelHandlerWrap {
	private String name;
	private ChannelHandler handler;

	public ChannelHandlerWrap(String name, ChannelHandler handler) {
		super();
		this.name = name;
		this.handler = handler;
	}

	public String getName() {
		return name;
	}

	public ChannelHandler getHandler() {
		return handler;
	}

}
