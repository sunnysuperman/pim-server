package com.github.sunnysuperman.pim.test;

import java.util.LinkedList;
import java.util.Map;

import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.commons.utils.StringUtil;
import com.github.sunnysuperman.pim.protocol.CommunicationPacket;

public class RecordLoginPacket extends CommunicationPacket {
	private Map<String, Object> context;

	public RecordLoginPacket(Map<String, Object> context) {
		super();
		this.context = context;
	}

	public Map<String, Object> getContext() {
		return context;
	}

	@Override
	public LinkedList<byte[]> encodeBody() {
		LinkedList<byte[]> body = new LinkedList<byte[]>();
		body.add(JSONUtil.toJSONString(context).getBytes(StringUtil.UTF8_CHARSET));
		return body;
	}

	public static RecordLoginPacket decode(byte[] data, int offset) {
		String contextAsString = new String(data, offset, data.length - offset, StringUtil.UTF8_CHARSET);
		Map<String, Object> context = JSONUtil.parseJSONObject(contextAsString);
		return new RecordLoginPacket(context);
	}

}
