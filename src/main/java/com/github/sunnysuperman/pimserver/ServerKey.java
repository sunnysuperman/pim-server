package com.github.sunnysuperman.pimserver;

import io.netty.util.AttributeKey;

public interface ServerKey {

	public static final AttributeKey<Session> KEY_SESSION = AttributeKey.valueOf("s");

	public static final AttributeKey<Boolean> KEY_COMPRESS = AttributeKey.valueOf("c");

}
