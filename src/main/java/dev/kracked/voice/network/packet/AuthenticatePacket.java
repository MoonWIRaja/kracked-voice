package dev.kracked.voice.network.packet;

import java.nio.ByteBuffer;
import java.util.UUID;

public class AuthenticatePacket implements Packet {
    private UUID playerUuid;
    private String playerName;
    private String secretKey;

    public AuthenticatePacket() {}

    public AuthenticatePacket(UUID playerUuid, String secretKey) {
        this.playerUuid = playerUuid;
        this.secretKey = secretKey;
    }

    @Override
    public void fromBytes(ByteBuffer buf) {
        long mostSig = buf.getLong();
        long leastSig = buf.getLong();
        this.playerUuid = new UUID(mostSig, leastSig);
        // FIXED: Read player name (client sends it)
        int nameLength = buf.getInt();
        if (nameLength > 0 && nameLength < 64) {
            byte[] nameBytes = new byte[nameLength];
            buf.get(nameBytes);
            this.playerName = new String(nameBytes);
        }
        // Secret key is sent by client but we use server's key instead
    }

    @Override
    public void toBytes(ByteBuffer buf) {
        buf.putLong(playerUuid.getMostSignificantBits());
        buf.putLong(playerUuid.getLeastSignificantBits());
        byte[] secretBytes = secretKey.getBytes();
        buf.putInt(secretBytes.length);
        buf.put(secretBytes);
    }

    @Override
    public byte getPacketId() {
        return 3;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getSecretKey() { return secretKey; }
}
