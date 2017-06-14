package com.github.sunnysuperman.pim.protocol;


public class DefaultPacket extends Packet {

	public DefaultPacket(byte type, byte[] body) {
		super(type);
		this.body = body;
	}

	@Override
	protected byte[] makeBody() {
		return null;
	}

}
