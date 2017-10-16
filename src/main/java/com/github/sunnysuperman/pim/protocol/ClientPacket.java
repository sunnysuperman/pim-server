package com.github.sunnysuperman.pim.protocol;

import java.util.LinkedList;

import com.github.sunnysuperman.commons.utils.StringUtil;
import com.github.sunnysuperman.pim.util.PacketUtil;

public class ClientPacket extends CommunicationPacket {
    private Packet packet;
    private ClientID clientID;

    public ClientPacket(Packet packet, ClientID clientID) {
        this.packet = packet;
        this.clientID = clientID;
    }

    public Packet getPacket() {
        return packet;
    }

    public ClientID getClientID() {
        return clientID;
    }

    @Override
    public LinkedList<byte[]> encodeBody() {
        LinkedList<byte[]> components = new LinkedList<byte[]>();
        byte[] innerBody = packet.getBody();
        components.add(PacketUtil.makeHeader(packet.getType(), innerBody == null ? 0 : innerBody.length, false));
        if (innerBody != null) {
            components.add(innerBody);
        }
        components.add(clientID.toString().getBytes(StringUtil.UTF8_CHARSET));
        return components;
    }

    public static ClientPacket decode(byte[] data, int offset) {
        byte metadata = data[offset];
        offset++;
        byte msgType = PacketUtil.getPacketType(metadata);
        boolean hasData = ((metadata >> 7) & 0x1) > 0;
        byte[] realBody = null;
        if (hasData) {
            int bodySize = (data[offset + 2] & 0xFF) + (data[offset + 1] & 0xFF) * 256 + (data[offset] & 0xFF) * 65536;
            offset += 3;
            if (bodySize > 0) {
                // route packet never compress
                realBody = new byte[bodySize];
                System.arraycopy(data, offset, realBody, 0, bodySize);
                offset += bodySize;
            }
        }
        Packet packet = new DefaultPacket(msgType, realBody);
        String clientIDAsString = new String(data, offset, data.length - offset, StringUtil.UTF8_CHARSET);
        ClientID clientID = ClientID.wrap(clientIDAsString);
        return new ClientPacket(packet, clientID);
    }

}
