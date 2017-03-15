package com.github.sunnysuperman.pimserver;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class Session {
	public static final int HEADER_LENGTH = 4;

	public final ByteBuffer headerBuf = ByteBuffer.allocate(HEADER_LENGTH);
	public LinkedList<ByteBuffer> bodyBufs;
	public int bodyBytes;
	public int bodyLeftBytes;

	public final void clearBuffer() {
		headerBuf.clear();
		bodyBufs = null;
		bodyBytes = 0;
		bodyLeftBytes = 0;
	}
}
