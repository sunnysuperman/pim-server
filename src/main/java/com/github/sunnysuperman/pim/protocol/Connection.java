package com.github.sunnysuperman.pim.protocol;

import io.netty.channel.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pim.util.Utils;

public class Connection {
    private static final Logger LOG = LoggerFactory.getLogger(Connection.class);
    protected final Channel channel;
    protected volatile int compressThresold;

    public Connection(Channel channel, int compressThresold) {
        this.channel = Utils.notNull(channel);
        this.compressThresold = compressThresold;
    }

    public Connection(Channel channel) {
        this(channel, 0);
    }

    public final Channel getChannel() {
        return channel;
    }

    // public void setChannel(Channel channel) {
    // this.channel = Utils.notNull(channel);
    // }

    public final int getCompressThresold() {
        return compressThresold;
    }

    public final void setCompressThresold(int compressThresold) {
        this.compressThresold = compressThresold;
    }

    public final boolean isSameConnection(Object o) {
        return o == channel;
    }

    public final boolean isActive() {
        return channel.isActive();
    }

    public final boolean close() {
        // if (channel == null) {
        // return false;
        // }
        try {
            // TODO need to check channel isActive
            channel.close();
            return true;
        } catch (Throwable t) {
            LOG.error(null, t);
            return false;
        }
    }

    public final boolean write(PacketOutput output) {
        try {
            channel.writeAndFlush(output.serialize(compressThresold));
            return true;
        } catch (Throwable t) {
            LOG.error(null, t);
            return false;
        }
    }

    public final boolean write(Packet packet) {
        try {
            PacketOutput output = new PacketOutput(packet.getType());
            byte[] body = packet.getBody();
            if (body != null) {
                output.appendBody(body);
            }
            return write(output);
        } catch (Throwable t) {
            LOG.error(null, t);
            return false;
        }
    }

}
