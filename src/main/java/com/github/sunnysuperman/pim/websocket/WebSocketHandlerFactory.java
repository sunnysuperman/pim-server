package com.github.sunnysuperman.pim.websocket;

import com.github.sunnysuperman.pim.bootstrap.ServerConfig;

public interface WebSocketHandlerFactory {

	WebSocketHandler createHandler(ServerConfig config);

}
