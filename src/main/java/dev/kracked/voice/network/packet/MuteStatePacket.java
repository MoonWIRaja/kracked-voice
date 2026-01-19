package dev.kracked.voice.network.packet;

import java.nio.ByteBuffer;

public class MuteStatePacket implements Packet {
    private boolean muted;

    public MuteStatePacket() {}

    public MuteStatePacket(boolean muted) {
        this.muted = muted;
    }

    @Override
    public void fromBytes(ByteBuffer buf) {
        this.muted = buf.get() == 1;
    }

    @Override
    public void toBytes(ByteBuffer buf) {
        buf.put((byte) (muted ? 1 : 0));
    }

    @Override
    public byte getPacketId() {
        return 9;
    }

    public boolean isMuted() { return muted; }
}
