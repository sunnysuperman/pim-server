package com.github.sunnysuperman.pim.protocol;


public class ConnectPacket extends Packet {

	public ConnectPacket(byte[] info) {
		super(PacketType.TYPE_CONNECT);
		this.body = info;
	}

	@Override
	protected byte[] makeBody() {
		return null;
	}

}
