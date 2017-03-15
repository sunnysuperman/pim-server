package com.github.sunnysuperman.pimserver.region;

import java.util.Collection;

import com.github.sunnysuperman.pimserver.packet.CommunicationPacket;

public interface RegionPacketRouter {

	Collection<Region> getRegions();

	String getMyRegionId();

	boolean routeToRegion(String regionId, CommunicationPacket packet, int maxTry);

	boolean routeToAll(CommunicationPacket packet, int maxTry);

	void stop(int seconds);

}
