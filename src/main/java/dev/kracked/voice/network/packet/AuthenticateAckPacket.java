package dev.kracked.voice.network.packet;

import java.nio.ByteBuffer;

public class AuthenticateAckPacket implements Packet {
    @Override
    public void fromBytes(ByteBuffer buf) {}

    @Override
    public void toBytes(ByteBuffer buf) {}

    @Override
    public byte getPacketId() {
        return 4;
    }
}
