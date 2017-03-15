package com.github.sunnysuperman.pimserver.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pimsdk.util.GZipUtil;
import com.github.sunnysuperman.pimsdk.util.PacketUtil;
import com.github.sunnysuperman.pimserver.util.Utils;

public class PacketOutput {
	private static final Logger LOG = LoggerFactory.getLogger(PacketOutput.class);
	private byte type;
	private List<byte[]> components;
	private int size = -1;
	private volatile boolean marshall;

	public PacketOutput(byte type) {
		this.type = type;
		this.components = new LinkedList<byte[]>();
	}

	public PacketOutput(byte type, List<byte[]> components) {
		this.type = type;
		this.components = Utils.notNull(components);
	}

	public final void appendBody(byte[] component) {
		if (component == null || component.length == 0) {
			throw new RuntimeException("component should not be empty");
		}
		if (marshall) {
			throw new RuntimeException("Already marshall");
		}
		components.add(component);
	}

	public void marshall() {
		marshall = true;
	}

	public int size() {
		marshall();
		if (size < 0) {
			int theSize = 0;
			for (byte[] component : components) {
				theSize += component.length;
			}
			size = theSize;
		}
		return size;
	}

	public final ByteBuf serialize(int compressThreshold) throws IOException {
		int bodySize = size();
		boolean compress = compressThreshold > 0 && bodySize > compressThreshold;
		ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
		if (compress) {
			byte[] body;
			// compress
			{
				if (components.size() > 1) {
					body = new byte[bodySize];
					int offset = 0, componentLen = 0;
					for (byte[] component : components) {
						componentLen = component.length;
						System.arraycopy(component, 0, body, offset, componentLen);
						offset += componentLen;
					}
				} else {
					body = components.get(0);
				}
				byte[] compressBody = GZipUtil.compress(body);
				if (compressBody.length < body.length) {
					body = compressBody;
				} else {
					LOG.warn("compress body size >= original body size, use original body instead");
					compress = false;
				}
			}
			byte[] header = PacketUtil.makeHeader(type, body.length, compress);
			ByteBuf buf = allocator.directBuffer(header.length + body.length);
			buf.writeBytes(header, 0, header.length);
			buf.writeBytes(body, 0, body.length);
			return buf;
		}
		byte[] header = PacketUtil.makeHeader(type, bodySize, false);
		ByteBuf buf = allocator.directBuffer(header.length + bodySize);
		buf.writeBytes(header, 0, header.length);
		for (byte[] component : components) {
			buf.writeBytes(component, 0, component.length);
		}
		return buf;
	}

	public final byte[] serializeDirectly(int compressThreshold) throws IOException {
		int bodySize = size();
		boolean compress = compressThreshold > 0 && bodySize > compressThreshold;
		if (compress) {
			byte[] body;
			// compress
			{
				if (components.size() > 1) {
					body = new byte[bodySize];
					int offset = 0, componentLen = 0;
					for (byte[] component : components) {
						componentLen = component.length;
						System.arraycopy(component, 0, body, offset, componentLen);
						offset += componentLen;
					}
				} else {
					body = components.get(0);
				}
				byte[] compressBody = GZipUtil.compress(body);
				if (compressBody.length < body.length) {
					body = compressBody;
				} else {
					LOG.warn("compress body size >= original body size, use original body instead");
					compress = false;
				}
			}
			byte[] header = PacketUtil.makeHeader(type, body.length, compress);
			byte[] buf = new byte[header.length + body.length];
			System.arraycopy(header, 0, buf, 0, header.length);
			System.arraycopy(body, 0, buf, header.length, body.length);
			return buf;
		}
		byte[] header = PacketUtil.makeHeader(type, bodySize, false);
		byte[] buf = new byte[header.length + bodySize];
		System.arraycopy(header, 0, buf, 0, header.length);
		int offset = header.length;
		for (byte[] component : components) {
			System.arraycopy(component, 0, buf, offset, component.length);
			offset += component.length;
		}
		return buf;
	}

}
