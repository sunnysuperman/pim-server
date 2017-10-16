package com.github.sunnysuperman.pim.client;

import io.netty.channel.ChannelHandlerContext;

import com.github.sunnysuperman.pim.protocol.ClientID;

public interface ClientAuthProvider {
    public static final int AUTH_OK = 0;

    public static class AuthResult {
        public ClientID clientID;
        public int errorCode;
        public boolean compressEnabled;
    }

    AuthResult auth(ChannelHandlerContext context, byte[] body);

}
