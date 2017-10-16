package com.github.sunnysuperman.pim.util.sequence;

public class TimeBasedSequenceIdGenerator implements SequenceIdGenerator {
    private final byte[] LOCK = new byte[0];
    private long[] sequenceID = null;

    @Override
    public String generate() {
        long t1, t2;
        synchronized (LOCK) {
            t1 = System.currentTimeMillis() / 1000L;
            if (sequenceID == null) {
                sequenceID = new long[] { t1, 1 };
            } else {
                if (sequenceID[0] == t1) {
                    sequenceID[1] = sequenceID[1] + 1;
                } else {
                    sequenceID[0] = t1;
                    sequenceID[1] = 1;
                }
            }
            t2 = sequenceID[1];
        }
        // 10 + 1 + 3
        return new StringBuilder(14).append(t1).append('-').append(t2).toString();
    }
}
