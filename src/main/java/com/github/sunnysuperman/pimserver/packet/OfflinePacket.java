package com.github.sunnysuperman.pimserver.packet;

import java.util.List;

import com.github.sunnysuperman.commons.utils.ByteUtil;
import com.github.sunnysuperman.pimsdk.ClientID;
import com.github.sunnysuperman.pimserver.cluster.RouteResult;

public class OfflinePacket extends OnlineOfflinePacket {

	public OfflinePacket(ClientID clientID, RouteResult route) {
		super(clientID, route);
	}

	public static OfflinePacket decode(byte[] data, int offset) {
		List<byte[]> components = decodeBodyAsComponents(data, offset);
		ClientID clientID = ClientID.wrap(ByteUtil.bytes2string(components.get(0)));
		RouteResult route = RouteResult.fromString(ByteUtil.bytes2string(components.get(1)));
		return new OfflinePacket(clientID, route);
	}
}
