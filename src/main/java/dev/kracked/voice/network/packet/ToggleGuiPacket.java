package dev.kracked.voice.network.packet;

import java.nio.ByteBuffer;

public class ToggleGuiPacket implements Packet {
    @Override
    public void fromBytes(ByteBuffer buf) {}

    @Override
    public void toBytes(ByteBuffer buf) {}

    @Override
    public byte getPacketId() {
        return 7;
    }
}
