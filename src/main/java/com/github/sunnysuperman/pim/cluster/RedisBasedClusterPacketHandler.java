package com.github.sunnysuperman.pim.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pim.client.ClientConnectionManager;
import com.github.sunnysuperman.pim.client.ClientPacketRouter;
import com.github.sunnysuperman.pim.protocol.ClientPacket;
import com.github.sunnysuperman.pim.protocol.CommunicationPacket;
import com.github.sunnysuperman.pim.protocol.Packet;
import com.github.sunnysuperman.pim.protocol.PacketType;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

public abstract class RedisBasedClusterPacketHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RedisBasedClusterPacketHandler.class);
    private ClientConnectionManager clientConnectionManager;
    private ClientPacketRouter packetRouter;

    public RedisBasedClusterPacketHandler(ClientConnectionManager clientConnectionManager,
            ClientPacketRouter packetRouter) {
        this.clientConnectionManager = clientConnectionManager;
        this.packetRouter = packetRouter;
    }

    private class ClusterPacketListener extends BinaryJedisPubSub {

        @Override
        public void onMessage(byte[] channel, byte[] message) {
            try {
                CommunicationPacket cPacket = CommunicationPacket.decode(message);
                if (cPacket instanceof ClientPacket) {
                    ClientPacket clientPacket = (ClientPacket) cPacket;
                    Packet payload = clientPacket.getPacket();
                    boolean routed = packetRouter.route(payload, clientPacket.getClientID(),
                            ClientPacketRouter.ROUTE_LOCAL) != ClientPacketRouter.ROUTE_NONE;
                    if (payload.getType() == PacketType.TYPE_DISCONNECT) {
                        try {
                            clientConnectionManager.removeAndCloseConnection(clientPacket.getClientID());
                        } catch (Throwable t) {
                            LOG.error(null, t);
                        }
                    } else {
                        if (!routed) {
                            try {
                                boolean removed = clientConnectionManager
                                        .removeFromRouteTableIfAbsent(clientPacket.getClientID());
                                if (removed) {
                                    LOG.warn("removeFromRouteTableIfAbsent: "
                                            + clientPacket.getClientID().getUsername());
                                }
                            } catch (Throwable t) {
                                LOG.error(null, t);
                            }
                        }
                    }
                    routeHandled(routed, clientPacket);
                }
            } catch (Throwable t) {
                LOG.error(null, t);
            }
        }

        @Override
        public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
            this.onMessage(channel, message);
        }

    }

    private void ensureSubcribe() {
        while (true) {
            Jedis jedis = null;
            try {
                jedis = getRedisClient();
                jedis.subscribe(new ClusterPacketListener(), clientConnectionManager.getMyServerId().getBytes());
                // will block until connection disconnected
            } catch (Exception e) {
                LOG.error(null, e);
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                // ignore
            }
        }
    }

    public void start() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                ensureSubcribe();
            }

        }).start();
    }

    protected abstract Jedis getRedisClient();

    protected abstract void routeHandled(boolean routed, ClientPacket clientPacket);

}
