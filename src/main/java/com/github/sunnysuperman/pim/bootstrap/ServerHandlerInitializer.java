package com.github.sunnysuperman.pim.bootstrap;

import java.util.List;

public interface ServerHandlerInitializer {

	List<ChannelHandlerWrap> createHandlers(ServerConfig config);

}
