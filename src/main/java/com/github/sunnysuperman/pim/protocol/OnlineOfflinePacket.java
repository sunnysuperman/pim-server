package com.github.sunnysuperman.pim.protocol;

import java.util.LinkedList;

import com.github.sunnysuperman.commons.utils.StringUtil;
import com.github.sunnysuperman.pim.cluster.RouteResult;
import com.github.sunnysuperman.pim.util.Utils;

public class OnlineOfflinePacket extends CommunicationPacket {
	private ClientID clientID;
	private RouteResult route;

	public OnlineOfflinePacket(ClientID clientID, RouteResult route) {
		this.clientID = Utils.notNull(clientID);
		this.route = Utils.notNull(route);
	}

	public ClientID getClientID() {
		return clientID;
	}

	public RouteResult getRoute() {
		return route;
	}

	@Override
	public LinkedList<byte[]> encodeBody() {
		LinkedList<byte[]> components = new LinkedList<byte[]>();
		appendWithLength(components, clientID.getUsername().getBytes(StringUtil.UTF8_CHARSET));
		appendWithLength(components, route.toString().getBytes(StringUtil.UTF8_CHARSET));
		return components;
	}
}
