package com.github.sunnysuperman.pim.protocol;

import com.github.sunnysuperman.pim.util.PimUtil;

public class DisconnectPacket extends Packet {
    public static final int ERROR_DUPLICATE = 1;
    private int errorCode;

    public DisconnectPacket(int errorCode) {
        super(PacketType.TYPE_DISCONNECT);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    protected byte[] makeBody() {
        return PimUtil.int2bytes(errorCode);
    }

    public static DisconnectPacket deserialize(byte[] body) {
        DisconnectPacket packet = new DisconnectPacket(PimUtil.bytes2int(body));
        packet.body = body;
        return packet;
    }

}
