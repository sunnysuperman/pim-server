package com.github.sunnysuperman.pimserver;

import javax.net.ssl.SSLContext;

import com.github.sunnysuperman.commons.config.Config;
import com.github.sunnysuperman.pimserver.PacketHandler.PacketHandlerConfig;

public final class ServerConfig extends PacketHandlerConfig {
	private volatile int port;
	private volatile int backlog;
	private volatile int readBufferSize;
	private volatile int writeBufferSize;
	private volatile int readIdleTimeMills;
	private volatile int waitPongTimeMills;
	private volatile SSLContext sslContext;
	private volatile byte sslMode;

	public ServerConfig(Config config, String name) {
		super(config, name);
	}

	@Override
	protected void initValues() {
		if (backlog <= 0) {
			backlog = 1024;
		}
		if (waitPongTimeMills <= 0) {
			waitPongTimeMills = 15000;
		}
		super.initValues();
	}

	@Override
	protected boolean validate(String key, Object value) {
		if (key.equals("waitPongTimeMills")) {
			if (readIdleTimeMills > 0) {
				Integer v = (Integer) value;
				return v > 0 && v < readIdleTimeMills - 3000;
			}
			return true;
		}
		return super.validate(key, value);
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getBacklog() {
		return backlog;
	}

	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	public int getReadBufferSize() {
		return readBufferSize;
	}

	public void setReadBufferSize(int readBufferSize) {
		this.readBufferSize = readBufferSize;
	}

	public int getWriteBufferSize() {
		return writeBufferSize;
	}

	public void setWriteBufferSize(int writeBufferSize) {
		this.writeBufferSize = writeBufferSize;
	}

	public int getReadIdleTimeMills() {
		return readIdleTimeMills;
	}

	public void setReadIdleTimeMills(int readIdleTimeMills) {
		this.readIdleTimeMills = readIdleTimeMills;
	}

	public int getWaitPongTimeMills() {
		return waitPongTimeMills;
	}

	public void setWaitPongTimeMills(int waitPongTimeMills) {
		this.waitPongTimeMills = waitPongTimeMills;
	}

	public SSLContext getSslContext() {
		return sslContext;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public byte getSslMode() {
		return sslMode;
	}

	public void setSslMode(byte sslMode) {
		this.sslMode = sslMode;
	}

}
