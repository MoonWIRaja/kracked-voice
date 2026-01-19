package dev.kracked.voice.network.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PacketRegistry {
    private static final Map<Byte, Supplier<? extends Packet>> idToPacket = new HashMap<>();
    private static final Map<Class<? extends Packet>, Byte> classToId = new HashMap<>();

    static {
        register((byte) 1, MicPacket.class, MicPacket::new);
        register((byte) 2, PlayerSoundPacket.class, PlayerSoundPacket::new);
        register((byte) 3, AuthenticatePacket.class, AuthenticatePacket::new);
        register((byte) 4, AuthenticateAckPacket.class, AuthenticateAckPacket::new);
        register((byte) 5, PingPacket.class, PingPacket::new);
        register((byte) 6, KeepAlivePacket.class, KeepAlivePacket::new);
        register((byte) 7, ToggleGuiPacket.class, ToggleGuiPacket::new);
        register((byte) 8, TogglePartyGuiPacket.class, TogglePartyGuiPacket::new);
        register((byte) 9, MuteStatePacket.class, MuteStatePacket::new);
    }

    private static void register(byte id, Class<? extends Packet> clazz, Supplier<? extends Packet> supplier) {
        idToPacket.put(id, supplier);
        classToId.put(clazz, id);
    }

    public static Packet constructPacket(byte id) {
        Supplier<? extends Packet> supplier = idToPacket.get(id);
        return supplier != null ? supplier.get() : null;
    }

    public static byte getPacketId(Class<? extends Packet> clazz) {
        return classToId.getOrDefault(clazz, (byte) -1);
    }
}
