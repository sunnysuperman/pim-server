package com.github.sunnysuperman.pim.protocol;


public class PongPacket extends Packet {

	public PongPacket() {
		super(PacketType.TYPE_PONG);
	}

	@Override
	protected byte[] makeBody() {
		return null;
	}
}
