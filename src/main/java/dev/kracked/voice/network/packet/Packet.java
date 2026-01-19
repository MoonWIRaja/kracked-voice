package dev.kracked.voice.network.packet;

import java.nio.ByteBuffer;

public interface Packet {
    void fromBytes(ByteBuffer buf);
    void toBytes(ByteBuffer buf);
    byte getPacketId();
}
