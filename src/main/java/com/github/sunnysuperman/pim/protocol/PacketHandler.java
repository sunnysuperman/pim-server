package com.github.sunnysuperman.pim.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.commons.config.Config;
import com.github.sunnysuperman.pim.Configuration;
import com.github.sunnysuperman.pim.util.GZipUtil;
import com.github.sunnysuperman.pim.util.PacketUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;

public abstract class PacketHandler extends ChannelInboundHandlerAdapter {
	public static class PacketHandlerConfig extends Configuration {
		protected volatile int bodyMaxSize;
		protected volatile int bodyBufferSize;
		protected volatile boolean channelLog;

		public PacketHandlerConfig(Config config, String name) {
			super(config, name);
		}

		@Override
		protected void initValues() {
			if (bodyMaxSize <= 0) {
				bodyMaxSize = 256 * 256 * 256 - 1;
			}
			if (bodyBufferSize <= 0) {
				bodyBufferSize = 4096;
			}
		}

		@Override
		protected boolean validate(String key, Object value) {
			if (key.equals("bodyMaxSize")) {
				return ((Integer) value) > 0;
			} else if (key.equals("bodyBufferSize")) {
				return ((Integer) value) > 0;
			}
			return true;
		}

		public int getBodyMaxSize() {
			return bodyMaxSize;
		}

		public void setBodyMaxSize(int bodyMaxSize) {
			this.bodyMaxSize = bodyMaxSize;
		}

		public int getBodyBufferSize() {
			return bodyBufferSize;
		}

		public void setBodyBufferSize(int bodyBufferSize) {
			this.bodyBufferSize = bodyBufferSize;
		}

		public boolean isChannelLog() {
			return channelLog;
		}

		public void setChannelLog(boolean channelLog) {
			this.channelLog = channelLog;
		}

	}

	private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);

	protected PacketHandlerConfig config;

	public PacketHandler(PacketHandlerConfig config) {
		super();
		this.config = config;
	}

	private void logChannel(ChannelHandlerContext ctx, String message) {
		String chStr = ctx.channel().toString();
		String msg = new StringBuilder(chStr.length() + message.length() + 1).append(chStr).append(' ').append(message)
				.toString();
		LOG.info(msg);
	}

	// @Override
	// public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	// // TODO clear buffer?
	// // Attribute<Session> sessionAttr = ctx.attr(NIOServerKey.KEY_SESSION);
	// // Session session = sessionAttr.get();
	// // if (session != null) {
	// // session.headerBuf.clear();
	// // }
	// }

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg == null) {
			if (config.channelLog) {
				logChannel(ctx, "Read null");
			}
			return;
		}
		boolean readEnd = false;
		ByteBuf buf = null;
		try {
			if (!(msg instanceof ByteBuf)) {
				if (config.channelLog) {
					logChannel(ctx, "Read end");
				}
				readEnd = true;
				return;
			}
			Attribute<PimBuffer> bufferAttr = ctx.channel().attr(ServerKey.KEY_BUFFER);
			PimBuffer pimBuffer = bufferAttr.get();
			if (pimBuffer == null) {
				pimBuffer = new PimBuffer();
				bufferAttr.set(pimBuffer);
			}
			buf = (ByteBuf) msg;
			int len = buf.readableBytes();
			if (config.channelLog) {
				logChannel(ctx, "Read " + len + " bytes");
			}
			if (len == 0) {
				return;
			}
			byte[] bytes = null;
			int offset = 0;
			// if (buf.hasArray()) {
			// bytes = buf.array();
			// offset = buf.arrayOffset();
			// } else {
			bytes = new byte[len];
			buf.getBytes(buf.readerIndex(), bytes);
			// }
			readEnd = !read(ctx, pimBuffer, bytes, offset);
		} catch (Throwable t) {
			LOG.error(null, t);
			readEnd = true;
		} finally {
			// if (buf.isReadable()) {
			// if (LOG_CHANNEL) {
			// log(ctx, "read buf will continue to use"));
			// }
			// } else {
			// }
			if (buf != null) {
				buf.release();
			}
			if (readEnd) {
				ctx.close();
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof IOException) {
			return;
		}
		LOG.error(null, cause);
	}

	private boolean processPacketAndReadNext(ChannelHandlerContext ctx, PimBuffer session, Packet packet, byte[] bytes,
			int offset) throws IOException {
		// TODO lock?
		session.clearBuffer();
		if (config.channelLog) {
			logChannel(ctx, "Handle packet " + packet.getType() + ",  "
					+ (packet.getBody() == null ? 0 : packet.getBody().length));
		}
		boolean ok = false;
		try {
			ok = handlePacket(ctx, packet);
		} catch (Throwable t) {
			LOG.error(null, t);
		}
		if (!ok) {
			if (config.channelLog) {
				logChannel(ctx, "Handle packet " + packet.getType() + " failed, so close the channel");
			}
			return false;
		}
		return read(ctx, session, bytes, offset);
	}

	private boolean read(ChannelHandlerContext ctx, PimBuffer session, byte[] bytes, int offset) throws IOException {
		if (offset >= bytes.length) {
			return true;
		}
		if (session.bodyBytes == 0) {
			// read header
			ByteBuffer headerBuf = session.headerBuf;
			if (headerBuf.position() == 0) {
				headerBuf.put(bytes[offset]);
				offset++;
			}
			byte metadata = headerBuf.get(0);
			byte msgType = PacketUtil.getPacketType(metadata);
			boolean hasData = ((metadata >> 7) & 0x1) > 0;
			if (!hasData) {
				Packet p = new DefaultPacket(msgType, null);
				// process a packet & continue read
				return processPacketAndReadNext(ctx, session, p, bytes, offset);
			}
			while (offset < bytes.length && headerBuf.position() < PimBuffer.HEADER_LENGTH) {
				headerBuf.put(bytes[offset]);
				offset++;
			}
			if (headerBuf.position() != PimBuffer.HEADER_LENGTH) {
				// continue read header
				return true;
			}
			// byte2, byte3, byte4
			int bodySize = (headerBuf.get(3) & 0xFF) + (headerBuf.get(2) & 0xFF) * 256
					+ (headerBuf.get(1) & 0xFF) * 65536;
			if (bodySize == 0) {
				Packet p = new DefaultPacket(msgType, null);
				// process a packet & continue read
				return processPacketAndReadNext(ctx, session, p, bytes, offset);
			}
			if (bodySize < 0 || bodySize > config.bodyMaxSize) {
				if (config.channelLog) {
					logChannel(ctx, "Bad body size");
				}
				return false;
			}
			// ready for body
			session.bodyBytes = bodySize;
			session.bodyLeftBytes = bodySize;
			session.bodyBufs = new LinkedList<ByteBuffer>();
			if (offset >= bytes.length) {
				return true;
			}
		}

		// read body
		LinkedList<ByteBuffer> bufs = session.bodyBufs;
		ByteBuffer buf = bufs.isEmpty() ? null : bufs.getLast();
		if (buf == null || buf.remaining() == 0) {
			buf = ByteBuffer.allocate(Math.min(session.bodyLeftBytes, config.bodyBufferSize));
			bufs.add(buf);
		}
		int nRead = Math.min(buf.remaining(), bytes.length - offset);
		buf.put(bytes, offset, nRead);
		offset += nRead;
		session.bodyLeftBytes -= nRead;
		if (session.bodyLeftBytes < 0) {
			if (config.channelLog) {
				logChannel(ctx, "data.bodyLeftBytes < 0");
			}
			return false;
		}
		if (session.bodyLeftBytes == 0) {
			byte[] body = new byte[session.bodyBytes];
			int bodyOffset = 0;
			int limit = 0;
			for (ByteBuffer bb : bufs) {
				bb.flip();
				limit = bb.limit();
				bb.get(body, bodyOffset, limit);
				bodyOffset += limit;
			}
			byte metadata = session.headerBuf.get(0);
			boolean compress = ((metadata >> 6) & 0x1) > 0;
			byte msgType = PacketUtil.getPacketType(metadata);
			if (compress) {
				byte[] realBody = GZipUtil.decompress(body);
				if (config.channelLog) {
					logChannel(ctx, "Receive compress data: " + body.length + "->" + realBody.length);
				}
				body = realBody;
			}
			Packet p = new DefaultPacket(msgType, body);
			// process a packet & continue read
			return processPacketAndReadNext(ctx, session, p, bytes, offset);
		}
		// continue read
		return read(ctx, session, bytes, offset);
	}

	protected abstract boolean handlePacket(ChannelHandlerContext context, Packet packet);
}
