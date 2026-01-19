package dev.kracked.voice.network;

import dev.kracked.voice.network.packet.*;
import dev.kracked.voice.player.VoicePlayer;
import dev.kracked.voice.player.VoicePlayerManager;
import dev.kracked.voice.proximity.ProximityEngine;
import dev.kracked.voice.security.AesEncryption;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * UDP Voice Server using Netty and Binary Protocol
 */
public class UdpVoiceServer extends SimpleChannelInboundHandler<DatagramPacket> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpVoiceServer.class);
    
    private final String bindAddress;
    private final int port;
    private final VoicePlayerManager playerManager;
    private final ProximityEngine proximityEngine;
    private final AesEncryption encryption;
    
    private final boolean proximityEnabled;
    private final boolean encryptionEnabled;
    
    private EventLoopGroup group;
    private Channel channel;
    private ScheduledExecutorService scheduler;
    
    public UdpVoiceServer(String bindAddress, int port, VoicePlayerManager playerManager, 
                          ProximityEngine proximityEngine,
                          String secretKey, boolean proximityEnabled, boolean encryptionEnabled) {
        this.bindAddress = bindAddress;
        this.port = port;
        this.playerManager = playerManager;
        this.proximityEngine = proximityEngine;
        
        if (encryptionEnabled) {
             this.encryption = new AesEncryption(secretKey);
        } else {
             this.encryption = null;
        }
        
        this.proximityEnabled = proximityEnabled;
        this.encryptionEnabled = encryptionEnabled;
    }
    
    public void start() throws Exception {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .option(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        ch.pipeline().addLast(UdpVoiceServer.this);
                    }
                });
        
        if (bindAddress != null && !bindAddress.isEmpty() && !bindAddress.equals("0.0.0.0")) {
             channel = bootstrap.bind(bindAddress, port).sync().channel();
        } else {
             channel = bootstrap.bind(port).sync().channel();
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(playerManager::cleanupTimeouts, 10, 10, TimeUnit.SECONDS);
        
        LOGGER.info("UDP Voice Server (Binary Protocol) started on {}:{}", bindAddress, port);
    }
    
    public void stop() {
        if (scheduler != null) scheduler.shutdown();
        if (channel != null) channel.close();
        if (group != null) group.shutdownGracefully();
        LOGGER.info("UDP Voice Server stopped");
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket datagram) {
        ByteBuf buf = datagram.content();
        InetSocketAddress sender = datagram.sender();
        
        if (buf.readableBytes() < (encryptionEnabled ? 13 : 1)) {
            return;
        }

        try {
            byte[] payload;
            if (encryptionEnabled) {
                byte[] encrypted = new byte[buf.readableBytes()];
                buf.readBytes(encrypted);
                payload = encryption.decrypt(encrypted);
                if (payload == null) {
                    LOGGER.warn("Failed to decrypt packet from {}", sender);
                    return;
                }
            } else {
                payload = new byte[buf.readableBytes()];
                buf.readBytes(payload);
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
            byte packetId = byteBuffer.get();
            Packet packet = PacketRegistry.constructPacket(packetId);
            
            if (packet == null) {
                LOGGER.warn("Unknown packet ID: {} from {}", packetId, sender);
                return;
            }

            packet.fromBytes(byteBuffer);
            handlePacket(ctx, sender, packet);

        } catch (Exception e) {
            LOGGER.error("Error handling packet from {}", sender, e);
        }
    }

    private void handlePacket(ChannelHandlerContext ctx, InetSocketAddress sender, Packet packet) {
        if (packet instanceof AuthenticatePacket) {
            handleAuthenticate(ctx, sender, (AuthenticatePacket) packet);
        } else if (packet instanceof MicPacket) {
            handleMicPacket(ctx, sender, (MicPacket) packet);
        } else if (packet instanceof PingPacket) {
            handlePing(ctx, sender, (PingPacket) packet);
        } else if (packet instanceof KeepAlivePacket) {
            handleKeepAlive(sender);
        }
    }
    
    // ==================== Packet Handlers ====================
    
    private void handleAuthenticate(ChannelHandlerContext ctx, InetSocketAddress sender, AuthenticatePacket packet) {
        UUID playerId = packet.getPlayerUuid();
        VoicePlayer player = playerManager.getPlayer(playerId);

        if (player == null) {
            // FIXED: Auto-register players who connect via UDP first
            // Use the player name from the auth packet
            String playerName = packet.getPlayerName();
            if (playerName == null || playerName.isEmpty()) {
                playerName = playerId.toString().substring(0, 8);
            }
            LOGGER.info("Auto-registering player via UDP: {} ({})", playerName, playerId);
            player = playerManager.registerPlayer(playerId, playerName);
        }

        player.setAddress(sender);
        player.setConnected(true);
        player.setLastPacketTime(System.currentTimeMillis());
        playerManager.setPlayerAddress(playerId, sender);

        sendPacket(ctx, sender, new AuthenticateAckPacket());
        LOGGER.info("Player authenticated: {} ({})", player.getPlayerName(), sender);
    }
    
    private void handleMicPacket(ChannelHandlerContext ctx, InetSocketAddress sender, MicPacket packet) {
        VoicePlayer speaker = playerManager.getPlayerByAddress(sender);
        if (speaker == null || !speaker.isConnected() || speaker.isMuted()) {
            return;
        }
        
        speaker.setLastPacketTime(System.currentTimeMillis());
        speaker.setTalking(true);
        
        List<VoicePlayer> listeners = playerManager.getPlayersInWorld(speaker.getWorldId());
        
        for (VoicePlayer listener : listeners) {
            if (listener.getPlayerId().equals(speaker.getPlayerId()) || listener.isDeafened() || listener.getAddress() == null) {
                continue;
            }
            
            double volume = 1.0;
            if (proximityEnabled) {
                volume = proximityEngine.calculateVolume(
                    listener.getX(), listener.getY(), listener.getZ(),
                    speaker.getX(), speaker.getY(), speaker.getZ()
                );
                if (volume <= 0) continue;
            }
            
            PlayerSoundPacket soundPacket = new PlayerSoundPacket(
                speaker.getPlayerId(),
                packet.getData(),
                packet.getSequenceNumber(),
                (float) volume,
                packet.isWhispering()
            );
            
            sendPacket(ctx, listener.getAddress(), soundPacket);
        }
    }
    
    private void handleKeepAlive(InetSocketAddress sender) {
        VoicePlayer player = playerManager.getPlayerByAddress(sender);
        if (player != null) {
            player.setLastPacketTime(System.currentTimeMillis());
            player.setTalking(false);
        }
    }
    
    private void handlePing(ChannelHandlerContext ctx, InetSocketAddress sender, PingPacket packet) {
        sendPacket(ctx, sender, packet); // Echo back the same ping packet
    }
    
    public void sendToggleGui(VoicePlayer player) {
        if (player != null && player.getAddress() != null) {
            sendPacket(channel, player.getAddress(), new ToggleGuiPacket());
        }
    }
    
    public void sendTogglePartyGui(VoicePlayer player) {
        if (player != null && player.getAddress() != null) {
            sendPacket(channel, player.getAddress(), new TogglePartyGuiPacket());
        }
    }

    public void sendMuteState(VoicePlayer player, boolean muted) {
        if (player != null && player.getAddress() != null) {
            sendPacket(channel, player.getAddress(), new MuteStatePacket(muted));
        }
    }

    private void sendPacket(ChannelHandlerContext ctx, InetSocketAddress address, Packet packet) {
        sendPacket(ctx.channel(), address, packet);
    }

    private void sendPacket(Channel channel, InetSocketAddress address, Packet packet) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2048); // Large enough for most packets
        byteBuffer.put(packet.getPacketId());
        packet.toBytes(byteBuffer);
        byteBuffer.flip();
        
        byte[] payload = new byte[byteBuffer.remaining()];
        byteBuffer.get(payload);
        
        if (encryptionEnabled) {
            payload = encryption.encrypt(payload);
        }
        
        if (payload != null) {
            channel.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(payload), address));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Voice server error", cause);
    }
}
