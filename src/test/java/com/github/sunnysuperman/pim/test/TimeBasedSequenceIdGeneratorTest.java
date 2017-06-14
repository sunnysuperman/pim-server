package com.github.sunnysuperman.pim.test;

import com.github.sunnysuperman.pim.util.sequence.TimeBasedSequenceIdGenerator;

import junit.framework.TestCase;

public class TimeBasedSequenceIdGeneratorTest extends TestCase {
	TimeBasedSequenceIdGenerator generator;

	@Override
	public void setUp() {
		generator = new TimeBasedSequenceIdGenerator();
	}

	private class GenerateSequenceThread extends Thread {
		public void run() {
			String sequenceId = generator.generate();
			System.out.println(sequenceId);
		}
	}

	public void test() throws Exception {
		for (int i = 0; i < 20; i++) {
			new GenerateSequenceThread().start();
		}
		Thread.sleep(1000);
		for (int i = 0; i < 10; i++) {
			new GenerateSequenceThread().start();
		}
	}

	public void test2() throws Exception {
		long t1 = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			String sequenceId = generator.generate();
			System.out.println(sequenceId);
		}
		long t2 = System.currentTimeMillis();
		System.out.println("Using " + (t2 - t1) + "ms");
	}
}
