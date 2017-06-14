package com.github.sunnysuperman.pim.protocol;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.pim.util.Utils;
import com.github.sunnysuperman.pim.util.sequence.SequenceIdGenerator;
import com.github.sunnysuperman.pim.util.sequence.TimeBasedSequenceIdGenerator;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.ScheduledFuture;

public class PacketSafeSender {

	private static class OutputPacketWrap {
		private SequenceIdAwarePacketOutput output;
		private long sendAt;
		private byte maxTry;
		private byte sendTimes;

		public OutputPacketWrap(SequenceIdAwarePacketOutput output, long sendAt, byte maxTry) {
			super();
			this.output = output;
			this.sendAt = sendAt;
			this.maxTry = maxTry;
			this.sendTimes = 1;
		}

	}

	private static final Logger LOG = LoggerFactory.getLogger(PacketSafeSender.class);
	private final byte[] PACKET_LOCK = new byte[0];
	private final byte[] CONNECTION_LOCK = new byte[0];
	private final EventLoopGroup loop;
	private final Map<String, OutputPacketWrap> pendingPackets = new HashMap<String, OutputPacketWrap>();
	private final SequenceIdGenerator sequenceIdGenerator;
	private volatile Connection connection;
	private volatile byte sendMaxTry;
	private volatile int writeTimeoutMills;
	private volatile ScheduledFuture<?> timer;
	private volatile boolean stopped = false;

	private class EnsureMessageSentTask implements Runnable {

		@Override
		public void run() {
			boolean ok = resendTimeoutPackets(false);
			scheduleResend(!ok);
		}

	}

	private void scheduleResend(boolean rightNow) {
		synchronized (CONNECTION_LOCK) {
			if (stopped) {
				return;
			}
			int writeTimeoutCheckMills;
			if (rightNow) {
				writeTimeoutCheckMills = 1000;
			} else {
				writeTimeoutCheckMills = Math.round(writeTimeoutMills / 2f);
				if (writeTimeoutCheckMills <= 1000) {
					writeTimeoutCheckMills = 1000;
				}
			}
			while (true) {
				try {
					timer = loop.schedule(new EnsureMessageSentTask(), writeTimeoutCheckMills, TimeUnit.MILLISECONDS);
					break;
				} catch (Throwable t) {
					LOG.error(null, t);
					try {
						Thread.sleep(1000);
					} catch (Throwable t2) {
						LOG.error(null, t2);
					}
				}
			}
		}
	}

	private boolean resendTimeoutPackets(boolean force) {
		try {
			List<PacketOutput> timeoutPackets = new LinkedList<PacketOutput>();
			synchronized (PACKET_LOCK) {
				if (pendingPackets.isEmpty()) {
					return true;
				}
				if (LOG.isInfoEnabled()) {
					LOG.info("resend pending packets - total count: " + pendingPackets.size());
				}
				boolean active = isActive();
				if (!active) {
					if (LOG.isWarnEnabled()) {
						LOG.warn("connection is lost, will schedule send task later");
					}
					return true;
				}
				long now = System.currentTimeMillis();
				long timestamp = now - writeTimeoutMills;
				for (Iterator<Entry<String, OutputPacketWrap>> iter = pendingPackets.entrySet().iterator(); iter
						.hasNext();) {
					Entry<String, OutputPacketWrap> entry = iter.next();
					OutputPacketWrap wrap = entry.getValue();
					if (force || wrap.sendAt < timestamp) {
						if (!force && wrap.sendTimes >= wrap.maxTry) {
							if (LOG.isWarnEnabled()) {
								LOG.warn("Send timeout, sequenceId: " + wrap.output.getSequenceId());
							}
							iter.remove();
							continue;
						}
						// increase send counter
						wrap.sendTimes++;
						// reset send time
						wrap.sendAt = now;
						if (LOG.isWarnEnabled()) {
							LOG.warn("will resend packet at " + wrap.sendTimes + " times, sequenceId: "
									+ wrap.output.getSequenceId());
						}
						timeoutPackets.add(wrap.output);
					}
				}
			}
			if (!timeoutPackets.isEmpty()) {
				for (PacketOutput packet : timeoutPackets) {
					connection.write(packet);
				}
			}
			return true;
		} catch (Throwable t) {
			LOG.error(null, t);
			return false;
		}
	}

	public PacketSafeSender(Connection connection, EventLoopGroup loop, int writeTimeoutMills, int sendMaxTimes) {
		this.connection = Utils.notNull(connection);
		this.loop = Utils.notNull(loop);
		this.sequenceIdGenerator = new TimeBasedSequenceIdGenerator();
		setWriteTimeoutMills(writeTimeoutMills);
		setSendMaxTry(sendMaxTimes);
		scheduleResend(false);
	}

	public final Connection getConnection() {
		return connection;
	}

	public final void setConnection(Connection connection) {
		Utils.notNull(connection);
		synchronized (CONNECTION_LOCK) {
			if (stopped) {
				throw new RuntimeException("Could not setConnection after SafeSender stopped");
			}
			if (this.connection != null) {
				this.connection.close();
			}
			this.connection = connection;
		}
		// force resend all pending packets after reconnected
		resendTimeoutPackets(true);
	}

	public final int getWriteTimeoutMills() {
		return writeTimeoutMills;
	}

	public final void setWriteTimeoutMills(int writeTimeoutMills) {
		if (writeTimeoutMills < 1000) {
			throw new IllegalArgumentException("writeTimeoutMills < 1000");
		}
		this.writeTimeoutMills = writeTimeoutMills;
	}

	public final int getSendMaxTry() {
		return sendMaxTry;
	}

	public final void setSendMaxTry(int sendMaxTry) {
		if (sendMaxTry <= 0 || sendMaxTry > Byte.MAX_VALUE) {
			throw new IllegalArgumentException("Invalid sendMaxTry (0 - 127).");
		}
		this.sendMaxTry = (byte) sendMaxTry;
	}

	public final boolean send(SequenceIdAwarePacketOutput output, int maxTry) {
		if (stopped) {
			return false;
		}
		byte theMaxTry = (maxTry <= 0 || maxTry > Byte.MAX_VALUE) ? sendMaxTry : (byte) maxTry;
		// append to queue
		boolean ok = false;
		String sequenceId = output.getSequenceId();
		if (sequenceId != null && theMaxTry > 1) {
			try {
				synchronized (PACKET_LOCK) {
					pendingPackets.put(sequenceId, new OutputPacketWrap(output, System.currentTimeMillis(), theMaxTry));
				}
				ok = true;
			} catch (Throwable t) {
				LOG.error(null, t);
			}
		}
		// send
		try {
			if (connection.isActive()) {
				connection.write(output);
				ok = true;
			}
		} catch (Throwable t) {
			LOG.error(null, t);
		}
		return ok;
	}

	public final boolean send(CommunicationPacket packet, int maxTry) {
		packet.setSequenceId(sequenceIdGenerator.generate());
		SequenceIdAwarePacketOutput sout = packet.encode();
		return send(sout, maxTry);
	}

	public final boolean flush(int timeoutMills) {
		long t1 = System.currentTimeMillis();
		while (true) {
			try {
				synchronized (PACKET_LOCK) {
					if (pendingPackets.isEmpty()) {
						return true;
					}
				}
				if (timeoutMills > 0 && System.currentTimeMillis() - t1 > timeoutMills) {
					return false;
				}
				resendTimeoutPackets(false);
				Thread.sleep(3000);
			} catch (Throwable t) {
				LOG.error(null, t);
			}
		}
	}

	public boolean stop(int timeoutMills) {
		synchronized (CONNECTION_LOCK) {
			if (stopped) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("Safe sender is already stopped.");
				}
				return false;
			}
			stopped = true;
			// stop resend timer (later flush will do resending)
			try {
				if (timer != null) {
					timer.cancel(false);
					timer = null;
				}
			} catch (Throwable t) {
				LOG.error(null, t);
			}
		}
		boolean flushed = flush(timeoutMills);
		// close connection after packets flushed.
		connection.close();
		return flushed;
	}

	public final void onPacketSent(String sequenceId) {
		synchronized (PACKET_LOCK) {
			pendingPackets.remove(sequenceId);
		}
	}

	public boolean isStopped() {
		return stopped;
	}

	public boolean isActive() {
		return connection.isActive();
	}
}
