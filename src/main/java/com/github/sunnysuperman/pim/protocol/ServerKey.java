package com.github.sunnysuperman.pim.protocol;

import io.netty.util.AttributeKey;

import com.github.sunnysuperman.pim.client.ClientSession;

public interface ServerKey {
    public static final AttributeKey<PimBuffer> KEY_BUFFER = AttributeKey.valueOf("b");
    public static final AttributeKey<ClientSession> KEY_SESSION = AttributeKey.valueOf("s");
    public static final AttributeKey<Boolean> KEY_WEBSOCKET = AttributeKey.valueOf("w");
}
