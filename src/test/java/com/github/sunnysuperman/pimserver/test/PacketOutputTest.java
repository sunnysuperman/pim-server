package com.github.sunnysuperman.pimserver.test;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.github.sunnysuperman.commons.utils.IpUtil;
import com.github.sunnysuperman.commons.utils.StringUtil;
import com.github.sunnysuperman.pimserver.packet.CommunicationPacketTypes;

public class PacketOutputTest extends TestCase {

	public void test() throws Exception {
		CommunicationPacketTypes.register(10004, RecordLoginPacket.class);

		Map<String, Object> context = new HashMap<String, Object>(7);
		context.put("username", "user.100146");
		context.put("resource", null);
		context.put("t1", System.currentTimeMillis());
		context.put("t2", System.currentTimeMillis() + 10);
		context.put("ip", IpUtil.ip2int("127.0.0.1"));
		context.put("server", IpUtil.ip2int("127.0.0.1"));
		context.put("region", "1");

		RecordLoginPacket packet = new RecordLoginPacket(context);
		{
			ByteBuf buf = packet.encode().serialize(0);
			int i = 0;
			byte[] bytes = new byte[buf.readableBytes()];
			while (buf.readableBytes() > 0) {
				bytes[i] = buf.readByte();
				i++;
			}
			System.out.println(StringUtil.join(bytes));
		}
		{
			byte[] bytes = packet.encode().serializeDirectly(0);
			System.out.println(StringUtil.join(bytes));
		}

	}

}
