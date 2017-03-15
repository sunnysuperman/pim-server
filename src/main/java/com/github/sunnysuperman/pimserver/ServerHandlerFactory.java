package com.github.sunnysuperman.pimserver;

public interface ServerHandlerFactory {

	ServerHandler createHandler(ServerConfig config);
}
