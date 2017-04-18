package com.github.sunnysuperman.pimserver.region;

import com.github.sunnysuperman.pimserver.packet.CommunicationPacket;

public interface RegionPacketRouter {

	int getRegionCount();

	String getMyRegionId();

	boolean routeToRegion(String regionId, CommunicationPacket packet, int maxTry);

	boolean routeToAll(CommunicationPacket packet, int maxTry);

	void stop(int seconds);

}
