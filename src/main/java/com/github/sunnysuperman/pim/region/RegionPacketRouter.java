package com.github.sunnysuperman.pim.region;

import com.github.sunnysuperman.pim.protocol.CommunicationPacket;

public interface RegionPacketRouter {

    int getRegionCount();

    String getMyRegionId();

    boolean routeToRegion(String regionId, CommunicationPacket packet, int maxTry);

    boolean routeToAll(CommunicationPacket packet, int maxTry);

    void stop(int seconds);

}
