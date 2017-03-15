package com.github.sunnysuperman.pimserver.util;

public class IntegerLock {

	private int value;

	public IntegerLock(int initialValue) {
		this.value = initialValue;
	}

	public IntegerLock() {
		this(0);
	}

	public synchronized boolean setValue(int newValue) {
		if (value != newValue) {
			value = newValue;
			notifyAll();
			return true;
		}
		return false;
	}

	public synchronized boolean waitUntilChanged(long msTimeout) throws InterruptedException {
		int oldValue = value;
		if (msTimeout == 0L) {
			if (value == oldValue) {
				wait();
			}
			return true;
		}
		// 只等待指定的时间
		long endTime = System.currentTimeMillis() + msTimeout;
		long msRemaining = msTimeout;
		while ((value == oldValue) && (msRemaining > 0L)) {
			wait(msRemaining);
			msRemaining = endTime - System.currentTimeMillis();
		}
		// 可能满足了条件(返回真),也可能已经超时了(返回假)
		return (value != oldValue);
	}

	public synchronized int getValue() {
		return value;
	}
}
