package com.github.sunnysuperman.pim.protocol;

import java.util.List;

public class SequenceIdAwarePacketOutput extends PacketOutput {
    private String sequenceId;

    public SequenceIdAwarePacketOutput(byte type) {
        super(type);
    }

    public SequenceIdAwarePacketOutput(byte type, List<byte[]> components) {
        super(type, components);
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

}
