package com.github.sunnysuperman.pimserver.component;

import com.github.sunnysuperman.pimserver.packet.CommunicationPacket;

public interface ComponentPacketRouter {

	boolean route(CommunicationPacket packet, int maxTry);

	void stop(int seconds);
}
