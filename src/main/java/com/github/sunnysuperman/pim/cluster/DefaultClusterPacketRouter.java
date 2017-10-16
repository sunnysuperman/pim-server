package com.github.sunnysuperman.pim.cluster;

import java.util.concurrent.ConcurrentHashMap;

import com.github.sunnysuperman.commons.config.Config;
import com.github.sunnysuperman.pim.protocol.CommunicationPacket;
import com.github.sunnysuperman.pim.protocol.Connection;
import com.github.sunnysuperman.pim.protocol.ConnectionBuilder;
import com.github.sunnysuperman.pim.protocol.DefaultPacketWriter;
import com.github.sunnysuperman.pim.protocol.Packet;
import com.github.sunnysuperman.pim.protocol.PacketHandler;
import com.github.sunnysuperman.pim.protocol.PacketType;
import com.github.sunnysuperman.pim.protocol.PongPacket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class DefaultClusterPacketRouter extends ConnectionBuilder implements ClusterPacketRouter {

    public static class ClusterClientConfig extends ConnectionBuilderConfig {
        public ClusterClientConfig(Config config, String name) {
            super(config, name);
        }

        private volatile int port;

        @Override
        protected boolean validate(String key, Object value) {
            if (key.equals("port")) {
                return ((Integer) value) > 0;
            }
            return super.validate(key, value);
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

    }

    private class ClusterClientHandler extends PacketHandler {
        private String ip;

        public ClusterClientHandler(PacketHandlerConfig config, String ip) {
            super(config);
            this.ip = ip;
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            LOG.warn("close cluster connection: " + ctx.channel());
        }

        @Override
        protected boolean handlePacket(ChannelHandlerContext context, Packet packet) {
            byte type = packet.getType();
            if (type == PacketType.TYPE_PING) {
                Connection connection = connections.get(ip);
                if (connection == null || connection.getChannel() != context.channel()) {
                    LOG.warn(
                            "No the same cluster connection, so do not reply pong. Maybe previous connection is not closed properly.");
                    return false;
                }
                DefaultPacketWriter.getInstance().write(new PongPacket(), context.channel());
                return true;
            }
            return true;
        }
    }

    private final ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<String, Connection>();

    public DefaultClusterPacketRouter(ClusterClientConfig config) {
        super(config);
    }

    private Connection ensureConnection(String host) throws Exception {
        Connection connection = connections.get(host);
        if (connection != null && connection.isActive()) {
            return connection;
        }
        synchronized (connections) {
            connection = connections.get(host);
            if (connection != null && connection.isActive()) {
                return connection;
            }
            connection = (Connection) build(host, ((ClusterClientConfig) config).port, host);
            if (connection == null) {
                return null;
            }
            connections.put(host, connection);
        }
        return connection;
    }

    @Override
    protected PacketHandler newPacketHandler(Object context) {
        return new ClusterClientHandler(config, (String) context);
    }

    @Override
    protected Connection setupConnection(Channel channel, Object context) {
        return new Connection(channel, 0);
    }

    @Override
    public boolean route(String serverId, CommunicationPacket packet) {
        int trial = 0;
        boolean ok = false;
        while (trial < 2) {
            try {
                Connection connection = ensureConnection(serverId);
                if (connection != null) {
                    ok = connection.write(packet.encode());
                }
            } catch (Throwable t) {
                LOG.error(null, t);
            }
            if (ok) {
                break;
            }
            trial++;
        }
        return ok;
    }

    @Override
    public void stop(int seconds) {

    }

}
