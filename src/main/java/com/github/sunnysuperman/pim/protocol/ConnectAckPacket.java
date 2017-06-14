package com.github.sunnysuperman.pim.protocol;


public class ConnectAckPacket extends Packet {

	public ConnectAckPacket(byte[] info) {
		super(PacketType.TYPE_CONNECT_ACK);
		this.body = info;
	}

	@Override
	protected byte[] makeBody() {
		return null;
	}

}
