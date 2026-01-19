package dev.kracked.voice.network.packet;

import java.nio.ByteBuffer;

public class PingPacket implements Packet {
    private long timestamp;

    public PingPacket() {}

    public PingPacket(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void fromBytes(ByteBuffer buf) {
        this.timestamp = buf.getLong();
    }

    @Override
    public void toBytes(ByteBuffer buf) {
        buf.putLong(timestamp);
    }

    @Override
    public byte getPacketId() {
        return 5;
    }

    public long getTimestamp() { return timestamp; }
}
