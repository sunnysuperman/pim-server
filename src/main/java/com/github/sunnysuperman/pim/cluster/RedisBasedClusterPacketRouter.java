package com.github.sunnysuperman.pim.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.commons.utils.StringUtil;
import com.github.sunnysuperman.pim.protocol.CommunicationPacket;

import redis.clients.jedis.Jedis;

public abstract class RedisBasedClusterPacketRouter implements ClusterPacketRouter {
    private static final Logger LOG = LoggerFactory.getLogger(RedisBasedClusterPacketRouter.class);

    @Override
    public boolean route(String serverId, CommunicationPacket packet) {
        byte[] packetAsBytes;
        try {
            // SequenceIdAwarePacketOutput output = packet.encode();
            // packetAsBytes = output.serializeDirectly(0);
            packet.setSequenceId(null);
            packetAsBytes = packet.encodeAsBytes();
        } catch (Throwable t) {
            LOG.error(null, t);
            return false;
        }
        byte[] publishKey = serverId.getBytes(StringUtil.UTF8_CHARSET);
        Jedis jedis = null;
        try {
            jedis = getRedisClient(serverId);
            jedis.publish(publishKey, packetAsBytes);
            return true;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public void stop(int seconds) {

    }

    protected abstract Jedis getRedisClient(String serverId);

}
