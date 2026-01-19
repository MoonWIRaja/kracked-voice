package dev.kracked.voice.network.packet;

import java.nio.ByteBuffer;
import java.util.UUID;

public class PlayerSoundPacket implements Packet {
    private UUID senderUuid;
    private byte[] data;
    private long sequenceNumber;
    private float distance;
    private boolean whispering;

    public PlayerSoundPacket() {}

    public PlayerSoundPacket(UUID senderUuid, byte[] data, long sequenceNumber, float distance, boolean whispering) {
        this.senderUuid = senderUuid;
        this.data = data;
        this.sequenceNumber = sequenceNumber;
        this.distance = distance;
        this.whispering = whispering;
    }

    @Override
    public void fromBytes(ByteBuffer buf) {
        this.senderUuid = new UUID(buf.getLong(), buf.getLong());
        this.sequenceNumber = buf.getLong();
        this.distance = buf.getFloat();
        this.whispering = buf.get() == 1;
        int length = buf.getInt();
        this.data = new byte[length];
        buf.get(this.data);
    }

    @Override
    public void toBytes(ByteBuffer buf) {
        buf.putLong(senderUuid.getMostSignificantBits());
        buf.putLong(senderUuid.getLeastSignificantBits());
        buf.putLong(sequenceNumber);
        buf.putFloat(distance);
        buf.put((byte) (whispering ? 1 : 0));
        buf.putInt(data.length);
        buf.put(data);
    }

    @Override
    public byte getPacketId() {
        return 2;
    }

    public UUID getSenderUuid() { return senderUuid; }
    public byte[] getData() { return data; }
    public long getSequenceNumber() { return sequenceNumber; }
    public float getDistance() { return distance; }
    public boolean isWhispering() { return whispering; }
}
