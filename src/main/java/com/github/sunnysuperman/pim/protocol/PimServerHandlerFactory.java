package com.github.sunnysuperman.pim.protocol;

import com.github.sunnysuperman.pim.bootstrap.ServerConfig;

public interface PimServerHandlerFactory {

	PacketHandler createHandler(ServerConfig config);

}
