package com.github.sunnysuperman.pim.protocol;

import io.netty.channel.EventLoopGroup;

import com.github.sunnysuperman.commons.config.Config;

public abstract class PacketSafeSenderBuilder extends ConnectionBuilder {

	public static class PacketSafeSenderConfig extends ConnectionBuilderConfig {

		private volatile int writeTimeoutMills;
		private volatile int sendMaxTry;
		private volatile int compressThreshold;

		public PacketSafeSenderConfig(Config config, String name) {
			super(config, name);
		}

		@Override
		protected boolean validate(String key, Object value) {
			if (key.equals("writeTimeoutMills")) {
				return ((Integer) value) > 1000;
			}
			if (key.equals("sendMaxTry")) {
				return ((Integer) value) > 0;
			}
			return super.validate(key, value);
		}

		public int getWriteTimeoutMills() {
			return writeTimeoutMills;
		}

		public void setWriteTimeoutMills(int writeTimeoutMills) {
			this.writeTimeoutMills = writeTimeoutMills;
		}

		public int getSendMaxTry() {
			return sendMaxTry;
		}

		public void setSendMaxTry(int sendMaxTry) {
			this.sendMaxTry = sendMaxTry;
		}

		public int getCompressThreshold() {
			return compressThreshold;
		}

		public void setCompressThreshold(int compressThreshold) {
			this.compressThreshold = compressThreshold;
		}

	}

	protected final EventLoopGroup loop;

	public PacketSafeSenderBuilder(PacketSafeSenderConfig config, EventLoopGroup loop) {
		super(config);
		this.loop = loop;
	}

	private PacketSafeSenderConfig theConfig() {
		return (PacketSafeSenderConfig) config;
	}

	protected final PacketSafeSender ensureSender(PacketSafeSender sender, final Object context) {
		if (sender != null && sender.isActive()) {
			return sender;
		}
		Connection connection = null;
		try {
			if (sender != null) {
				if (sender.isActive()) {
					return sender;
				}
				if (sender.isStopped()) {
					return null;
				}
			}
			connection = buildConnection(context);
			if (connection == null) {
				return sender;
			}
			if (sender == null) {
				sender = new PacketSafeSender(connection, loop, theConfig().writeTimeoutMills, theConfig().sendMaxTry);
			} else {
				sender.setConnection(connection);
			}
			return sender;
		} catch (Throwable t) {
			LOG.error(null, t);
			if (connection != null) {
				connection.close();
			}
			if (sender != null) {
				sender.stop(1);
			}
			return null;
		}
	}

	protected abstract Connection buildConnection(Object context);
}
