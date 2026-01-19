package dev.kracked.voice.network.packet;

import java.nio.ByteBuffer;

public class MicPacket implements Packet {
    private byte[] data;
    private long sequenceNumber;
    private boolean whispering;

    public MicPacket() {}

    public MicPacket(byte[] data, long sequenceNumber, boolean whispering) {
        this.data = data;
        this.sequenceNumber = sequenceNumber;
        this.whispering = whispering;
    }

    @Override
    public void fromBytes(ByteBuffer buf) {
        this.sequenceNumber = buf.getLong();
        this.whispering = buf.get() == 1;
        int length = buf.getInt();
        this.data = new byte[length];
        buf.get(this.data);
    }

    @Override
    public void toBytes(ByteBuffer buf) {
        buf.putLong(sequenceNumber);
        buf.put((byte) (whispering ? 1 : 0));
        buf.putInt(data.length);
        buf.put(data);
    }

    @Override
    public byte getPacketId() {
        return 1;
    }

    public byte[] getData() { return data; }
    public long getSequenceNumber() { return sequenceNumber; }
    public boolean isWhispering() { return whispering; }
}
