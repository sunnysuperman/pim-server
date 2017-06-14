package com.github.sunnysuperman.pim.client;

import com.github.sunnysuperman.pim.protocol.ClientID;

public class ClientSession {
	private boolean compressEnabled;
	private ClientID clientID;

	public boolean isCompressEnabled() {
		return compressEnabled;
	}

	public void setCompressEnabled(boolean compressEnabled) {
		this.compressEnabled = compressEnabled;
	}

	public ClientID getClientID() {
		return clientID;
	}

	public void setClientID(ClientID clientID) {
		this.clientID = clientID;
	}

}
