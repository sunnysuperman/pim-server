package com.github.sunnysuperman.pim.component;

import com.github.sunnysuperman.pim.protocol.CommunicationPacket;

public interface ComponentPacketRouter {

	boolean route(CommunicationPacket packet, int maxTry);

	void stop(int seconds);
}
