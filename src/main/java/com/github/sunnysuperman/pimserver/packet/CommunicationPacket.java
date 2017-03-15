package com.github.sunnysuperman.pimserver.packet;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.github.sunnysuperman.commons.utils.ByteUtil;
import com.github.sunnysuperman.commons.utils.StringUtil;

public abstract class CommunicationPacket {
	private int type;
	private String sequenceId;

	public CommunicationPacket() {
		this.type = CommunicationPacketTypes.getType(this.getClass());
	}

	public final int getType() {
		return type;
	}

	public String getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(String sequenceId) {
		this.sequenceId = sequenceId;
	}

	public abstract LinkedList<byte[]> encodeBody();

	protected final void appendWithLength(List<byte[]> components, byte[] component) {
		int len = component == null ? 0 : component.length;
		components.add(ByteUtil.int2bytes(len));
		if (len > 0) {
			components.add(component);
		}
	}

	public final LinkedList<byte[]> encodeAsComponents() {
		LinkedList<byte[]> components = encodeBody();
		if (components == null) {
			components = new LinkedList<byte[]>();
		}
		components.addFirst(ByteUtil.int2bytes(type));
		if (sequenceId != null) {
			byte[] sequenceIdBytes = ByteUtil.string2bytes(sequenceId);
			components.addFirst(sequenceIdBytes);
			components.addFirst(new byte[] { (byte) sequenceIdBytes.length });
		} else {
			components.addFirst(new byte[] { 0 });
		}
		return components;
	}

	public final byte[] encodeAsBytes() {
		LinkedList<byte[]> components = encodeAsComponents();
		if (components.size() == 1) {
			return components.get(0);
		}
		int size = 0;
		for (byte[] component : components) {
			size += component.length;
		}
		byte[] dest = new byte[size];
		int offset = 0;
		for (byte[] component : components) {
			System.arraycopy(component, 0, dest, offset, component.length);
			offset += component.length;
		}
		return dest;
	}

	public final SequenceIdAwarePacketOutput encode() {
		LinkedList<byte[]> bytes = encodeAsComponents();
		SequenceIdAwarePacketOutput output = new SequenceIdAwarePacketOutput(ServerPacketType.TYPE_COMMUNICATION,
				bytes);
		output.setSequenceId(sequenceId);
		return output;
	}

	public static final CommunicationPacket decode(byte[] body) throws Exception {
		byte sequenceIdLength = body[0];
		int offset = 1;
		String sequenceId = null;
		if (sequenceIdLength > 0) {
			sequenceId = new String(body, offset, sequenceIdLength, StringUtil.UTF8_CHARSET);
			offset += sequenceIdLength;
		}
		int type = ByteUtil.bytes2int(body, offset);
		offset += 4;
		Class<? extends CommunicationPacket> clazz = CommunicationPacketTypes.getClazz(type);
		CommunicationPacket p = (CommunicationPacket) clazz.getMethod("decode", byte[].class, int.class).invoke(null,
				body, offset);
		p.setSequenceId(sequenceId);
		return p;
	}

	public static final List<byte[]> decodeBodyAsComponents(byte[] data, int offset) {
		List<byte[]> components = new LinkedList<byte[]>();
		while (offset < data.length) {
			int len = ByteUtil.bytes2int(Arrays.copyOfRange(data, offset, offset + 4));
			offset += 4;
			if (len > 0) {
				byte[] component = Arrays.copyOfRange(data, offset, offset + len);
				components.add(component);
				offset += len;
			} else {
				components.add(null);
			}
		}
		return components;
	}

}
