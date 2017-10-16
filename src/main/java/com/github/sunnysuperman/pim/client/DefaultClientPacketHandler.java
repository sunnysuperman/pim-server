package com.github.sunnysuperman.pim.client;

import com.github.sunnysuperman.pim.bootstrap.ServerConfig;
import com.github.sunnysuperman.pim.protocol.Packet;
import com.github.sunnysuperman.pim.protocol.PacketHandler;

import io.netty.channel.ChannelHandlerContext;

public class DefaultClientPacketHandler extends PacketHandler {
    protected DefaultClientPacketProcessor processor;

    public DefaultClientPacketHandler(ServerConfig config, DefaultClientPacketProcessor processor) {
        super(config);
        this.processor = processor;
    }

    @Override
    public final boolean handlePacket(ChannelHandlerContext channel, Packet packet) {
        return processor.handlePacket(channel, packet);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        processor.channelActive(ctx);
    }

    @Override
    public final void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        processor.channelInactive(ctx);
    }

}
