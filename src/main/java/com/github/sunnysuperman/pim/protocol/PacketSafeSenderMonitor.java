package com.github.sunnysuperman.pim.protocol;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.ScheduledFuture;

public abstract class PacketSafeSenderMonitor {
	protected static final Logger LOG = LoggerFactory.getLogger(PacketSafeSenderMonitor.class);
	private final byte[] LOCK = new byte[0];
	private final EventLoopGroup loop;
	private volatile ScheduledFuture<?> timer;
	private volatile boolean stopped;

	public PacketSafeSenderMonitor(EventLoopGroup loop) {
		this.loop = loop == null ? new NioEventLoopGroup(1) : loop;
		ensureSafeSenderLoop();
	}

	private class EnsureSafeSenderTask implements Runnable {

		@Override
		public void run() {
			ensureSafeSenderLoop();
		}

	}

	private void ensureSafeSenderLoop() {
		try {
			boolean ok = ensureSafeSender();
			synchronized (LOCK) {
				if (stopped) {
					return;
				}
				if (timer != null) {
					timer.cancel(false);
					timer = null;
				}
				if (ok) {
					timer = loop.schedule(new EnsureSafeSenderTask(), 60, TimeUnit.SECONDS);
				} else {
					timer = loop.schedule(new EnsureSafeSenderTask(), 1, TimeUnit.SECONDS);
				}
			}
		} catch (Throwable t) {
			LOG.error(null, t);
			try {
				Thread.sleep(1000);
			} catch (Throwable t2) {
				LOG.error(null, t2);
			}
			ensureSafeSenderLoop();
		}
	}

	public void stop() {
		synchronized (LOCK) {
			stopped = true;
			if (timer != null) {
				timer.cancel(false);
				timer = null;
			}
		}
	}

	public void alarm() {
		ensureSafeSenderLoop();
	}

	protected abstract boolean ensureSafeSender();

}
