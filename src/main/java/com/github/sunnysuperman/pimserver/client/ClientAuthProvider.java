package com.github.sunnysuperman.pimserver.client;

import io.netty.channel.ChannelHandlerContext;

import com.github.sunnysuperman.pimsdk.ClientID;
import com.github.sunnysuperman.pimsdk.Packet;

public interface ClientAuthProvider {
	public static final int AUTH_OK = 0;

	public static class AuthResult {
		public ClientID clientID;
		public int errorCode;
		public boolean compressEnabled;
	}

	AuthResult auth(ChannelHandlerContext context, Packet request);

}
