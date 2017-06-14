package com.github.sunnysuperman.pim.protocol;


public class PingPacket extends Packet {

	public PingPacket() {
		super(PacketType.TYPE_PING);
	}

	@Override
	protected byte[] makeBody() {
		return null;
	}
}
