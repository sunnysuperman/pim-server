package com.github.sunnysuperman.pim.region;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pim.bootstrap.ServerConfig;
import com.github.sunnysuperman.pim.client.ClientConnectionManager;
import com.github.sunnysuperman.pim.client.ClientPacketRouter;
import com.github.sunnysuperman.pim.protocol.ClientPacket;
import com.github.sunnysuperman.pim.protocol.CommunicationPacket;
import com.github.sunnysuperman.pim.protocol.DefaultPacketWriter;
import com.github.sunnysuperman.pim.protocol.MessageAck;
import com.github.sunnysuperman.pim.protocol.OfflinePacket;
import com.github.sunnysuperman.pim.protocol.OnlinePacket;
import com.github.sunnysuperman.pim.protocol.Packet;
import com.github.sunnysuperman.pim.protocol.PacketHandler;
import com.github.sunnysuperman.pim.protocol.PacketType;
import com.github.sunnysuperman.pim.protocol.PongPacket;
import com.github.sunnysuperman.pim.protocol.ServerPacketType;

import io.netty.channel.ChannelHandlerContext;

public class DefaultRegionPacketHandler extends PacketHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRegionPacketHandler.class);
    protected ClientConnectionManager clientConnectionManager;
    protected ClientPacketRouter packetRouter;

    public DefaultRegionPacketHandler(ServerConfig config, ClientConnectionManager clientConnectionManager,
            ClientPacketRouter packetRouter) {
        super(config);
        this.clientConnectionManager = clientConnectionManager;
        this.packetRouter = packetRouter;
    }

    @Override
    protected boolean handlePacket(ChannelHandlerContext channel, Packet packet) {
        byte type = packet.getType();
        if (type == ServerPacketType.TYPE_COMMUNICATION) {
            try {
                CommunicationPacket cPacket = CommunicationPacket.decode(packet.getBody());
                if (cPacket.getSequenceId() != null) {
                    MessageAck ack = new MessageAck(cPacket.getSequenceId(), null);
                    DefaultPacketWriter.getInstance().write(ack, channel.channel());
                }
                if (cPacket instanceof ClientPacket) {
                    ClientPacket clientPacket = (ClientPacket) cPacket;
                    Packet payload = clientPacket.getPacket();
                    int routed = packetRouter.route(payload, clientPacket.getClientID(),
                            ClientPacketRouter.ROUTE_CLUSTER);
                    boolean routeLocal = (routed & ClientPacketRouter.ROUTE_LOCAL) != 0;
                    if (routeLocal && payload.getType() == PacketType.TYPE_DISCONNECT) {
                        clientConnectionManager.removeAndCloseConnection(clientPacket.getClientID());
                    }
                } else if (cPacket instanceof OnlinePacket) {
                    OnlinePacket onlinePacket = (OnlinePacket) cPacket;
                    clientConnectionManager.onRemoteLogin(onlinePacket);
                } else if (cPacket instanceof OfflinePacket) {
                    OfflinePacket offlinePacket = (OfflinePacket) cPacket;
                    clientConnectionManager.onRemoteLogout(offlinePacket);
                } else {
                    LOG.error("No handler for packet: " + cPacket.getClass());
                }
            } catch (Throwable t) {
                LOG.error(null, t);
            }
            return true;
        }
        if (type == ServerPacketType.TYPE_PONG) {
            return true;
        }
        if (type == ServerPacketType.TYPE_PING) {
            DefaultPacketWriter.getInstance().write(new PongPacket(), channel.channel());
            return true;
        }
        return false;
    }
}
