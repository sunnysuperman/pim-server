package com.github.sunnysuperman.pim.component;

import com.github.sunnysuperman.commons.config.Config;
import com.github.sunnysuperman.commons.config.Config.ConfigValueChangedListener;
import com.github.sunnysuperman.commons.utils.StringUtil;
import com.github.sunnysuperman.pim.protocol.CommunicationPacket;
import com.github.sunnysuperman.pim.protocol.Connection;
import com.github.sunnysuperman.pim.protocol.DefaultPacketWriter;
import com.github.sunnysuperman.pim.protocol.MessageAck;
import com.github.sunnysuperman.pim.protocol.Packet;
import com.github.sunnysuperman.pim.protocol.PacketHandler;
import com.github.sunnysuperman.pim.protocol.PacketSafeSender;
import com.github.sunnysuperman.pim.protocol.PacketSafeSenderBuilder;
import com.github.sunnysuperman.pim.protocol.PacketSafeSenderMonitor;
import com.github.sunnysuperman.pim.protocol.PacketType;
import com.github.sunnysuperman.pim.protocol.PongPacket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;

public abstract class DefaultComponentPacketRouter extends PacketSafeSenderBuilder implements ComponentPacketRouter {

	public static class ComponentClientConfig extends PacketSafeSenderConfig {

		public ComponentClientConfig(Config config, String name) {
			super(config, name);
		}

		private volatile String host;
		private volatile int port;

		@Override
		protected boolean validate(String key, Object value) {
			if (key.equals("host")) {
				return StringUtil.isNotEmpty((String) value);
			}
			if (key.equals("port")) {
				return ((Integer) value) > 0;
			}
			return super.validate(key, value);
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

	}

	private class ComponentClientHandler extends PacketHandler {

		public ComponentClientHandler(PacketHandlerConfig config) {
			super(config);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
			LOG.warn("close component connection: " + ctx.channel());
			senderMonitor.alarm();
		}

		@Override
		protected boolean handlePacket(ChannelHandlerContext context, Packet packet) {
			try {
				PacketSafeSender sender = DefaultComponentPacketRouter.this.sender;
				if (sender == null) {
					LOG.warn("Receive packet before sender is setup");
					return true;
				}
				byte type = packet.getType();
				switch (type) {
				case PacketType.TYPE_PING: {
					if (sender.getConnection().getChannel() != context.channel()) {
						LOG.warn(
								"No the same component connection, so do not reply pong, maybe previous connection is not closed properly.");
						return false;
					}
					DefaultPacketWriter.getInstance().write(new PongPacket(), context.channel());
					return true;
				}
				case PacketType.TYPE_MSG_ACK: {
					MessageAck ack = MessageAck.deserialize(packet.getBody());
					sender.onPacketSent(ack.getSequenceID());
					return true;
				}
				default:
					onPacket(packet);
					break;
				}
				return true;
			} catch (Throwable t) {
				LOG.error(null, t);
				return true;
			}
		}
	}

	private final byte[] LOCK = new byte[0];
	private final PacketSafeSenderMonitor senderMonitor;
	private volatile PacketSafeSender sender;

	public DefaultComponentPacketRouter(ComponentClientConfig config) {
		super(config, new NioEventLoopGroup(1));
		this.senderMonitor = new PacketSafeSenderMonitor(this.loop) {

			@Override
			protected boolean ensureSafeSender() {
				if (sender != null && sender.getConnection().isActive()) {
					return true;
				}
				synchronized (LOCK) {
					sender = ensureSender(sender, null);
					return sender != null && sender.getConnection().isActive();
				}
			}

		};
		config.addListener(new ConfigValueChangedListener() {

			@Override
			public void onChanged(String key, Object value) {
				if (key.equals("writeTimeoutMills") || key.equals("sendMaxTry")) {
					int writeTimeoutMills = getConfig().getWriteTimeoutMills();
					int sendMaxTry = getConfig().getSendMaxTry();
					synchronized (LOCK) {
						if (sender != null) {
							sender.setWriteTimeoutMills(writeTimeoutMills);
							sender.setSendMaxTry(sendMaxTry);
						}
					}
				}
			}

		});
	}

	private ComponentClientConfig getConfig() {
		return (ComponentClientConfig) config;
	}

	protected abstract void onPacket(Packet packet);

	@Override
	protected Connection buildConnection(Object context) {
		return build(getConfig().getHost(), getConfig().port, context);
	}

	@Override
	public boolean route(CommunicationPacket packet, int maxTry) {
		PacketSafeSender theSender = sender;
		if (theSender != null) {
			return theSender.send(packet, maxTry);
		}
		return false;
	}

	@Override
	public void stop(int seconds) {
		senderMonitor.stop();
		synchronized (LOCK) {
			if (sender != null) {
				sender.stop(seconds * 1000);
			}
		}
	}

	@Override
	protected PacketHandler newPacketHandler(Object context) {
		return new ComponentClientHandler(config);
	}

	@Override
	protected Connection setupConnection(Channel channel, Object context) {
		return new Connection(channel);
	}

}
