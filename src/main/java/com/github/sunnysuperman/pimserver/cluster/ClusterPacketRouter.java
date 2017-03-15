package com.github.sunnysuperman.pimserver.cluster;

import com.github.sunnysuperman.pimserver.packet.CommunicationPacket;

public interface ClusterPacketRouter {

	String getMyServerId();

	boolean route(String serverId, CommunicationPacket packet);

	void stop(int seconds);
}
