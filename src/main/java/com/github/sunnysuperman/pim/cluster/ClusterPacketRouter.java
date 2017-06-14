package com.github.sunnysuperman.pim.cluster;

import com.github.sunnysuperman.pim.protocol.CommunicationPacket;

public interface ClusterPacketRouter {

	boolean route(String serverId, CommunicationPacket packet);

	void stop(int seconds);
}
