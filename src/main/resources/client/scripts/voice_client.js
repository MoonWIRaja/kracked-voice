/**
 * KrackedVoice - Premium Proximity Voice Chat for Hytale
 * By MoonWiRaja KRACKEDDEVS
 */

// ==================== Configuration ====================
const CONFIG = {
    voicePort: 3012, // FIXED: Match server port 3012
    sampleRate: 48000,
    frameSize: 960,
    channels: 1,
    proximityDistance: 50,
    syncTag: '[KRACKED_SYNC]',
    syncDelimiter: '|||' // FIXED: Use special delimiter to avoid Base64 conflicts
};

const PacketId = {
    MIC: 1,
    PLAYER_SOUND: 2,
    AUTHENTICATE: 3,
    AUTHENTICATE_ACK: 4,
    PING: 5,
    KEEP_ALIVE: 6,
    TOGGLE_GUI: 7,
    TOGGLE_PARTY_GUI: 8,
    MUTE_STATE: 9
};

// ==================== State Management ====================
const state = {
    connected: false,
    muted: false,
    deafened: false,
    talking: false,
    inputVolume: 1.0,
    outputVolume: 1.0,
    serverAddress: null,
    playerId: null,
    playerName: null,
    secretKey: "KRACKED_DEFAULT_KEY", 
    sequenceNumber: 0,
    reconnectTimer: null,
    keepAliveTimer: null
};

let socket = null;
let audioContext = null;
let microphoneStream = null;
const audioPlayers = new Map();

// ==================== Core Logic ====================

function onPlayerJoinServer(player) {
    state.playerId = player.id;
    state.playerName = player.name;
    state.serverAddress = player.serverAddress;

    console.log(`%c[KrackedVoice] %cWelcome, ${state.playerName}! Initializing voice...`, 'color: #8a2be2; font-weight: bold;', 'color: inherit;');

    // VISIBLE CONFIRMATION: Tell the user the script is running
    if (Hytale.ui && Hytale.ui.chat) {
        Hytale.ui.chat.pushMessage("§d§l[KrackedVoice] §fClient Script v4.1 Loaded! Connecting to port " + CONFIG.voicePort + "...");
    }

    initAudio();
    connectToVoiceServer();
}

function onPlayerLeaveServer() {
    disconnect();
}

function initAudio() {
    if (audioContext) return;
    audioContext = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: CONFIG.sampleRate });
    
    navigator.mediaDevices.getUserMedia({ audio: { 
        echoCancellation: true, 
        noiseSuppression: true, 
        autoGainControl: true 
    }}).then(stream => {
        microphoneStream = stream;
        const source = audioContext.createMediaStreamSource(stream);
        const processor = audioContext.createScriptProcessor(CONFIG.frameSize, 1, 1);
        
        processor.onaudioprocess = (e) => {
            if (state.connected && state.talking && !state.muted) {
                sendMicPacket(e.inputBuffer.getChannelData(0));
            }
        };
        
        source.connect(processor);
        processor.connect(audioContext.destination);
    }).catch(err => {
        console.error('[KrackedVoice] Microphone access denied:', err);
    });
}

function connectToVoiceServer() {
    let host = state.serverAddress;

    // Check if the address is internal or missing, then fallback to window.location
    const isInternal = (ip) => {
        if (!ip) return true;
        if (ip === 'localhost' || ip === '127.0.0.1' || ip === '0.0.0.0') return true;
        if (ip.startsWith('10.') || ip.startsWith('192.168.') || ip.startsWith('172.')) return true;
        return false;
    };

    if (isInternal(host)) {
        if (typeof window !== 'undefined' && window.location.hostname) {
            console.log(`[KrackedVoice] Internal IP detected (${host}), falling back to web host: ${window.location.hostname}`);
            host = window.location.hostname;
        }
    }

    if (!host) host = 'localhost';

    console.log(`%c[KrackedVoice] %cEstablishing UDP link to ${host}:${CONFIG.voicePort}...`, 'color: #8a2be2; font-weight: bold;', 'color: #fff;');

    try {
        socket = Hytale.network.createUdpSocket(`${host}:${CONFIG.voicePort}`);

        socket.onConnect = () => {
            console.log('%c[KrackedVoice] %cUDP Bridge Active. Authenticating...', 'color: #8a2be2; font-weight: bold;', 'color: #00ff00;');
            sendAuthPacket();
        };

        socket.onMessage = (data) => handlePacket(data);

        socket.onError = (err) => {
            console.error('[KrackedVoice] Bridge Error:', err);
            scheduleReconnect();
        };

        socket.connect();
    } catch (e) {
        console.error('[KrackedVoice] Failed to create UDP socket:', e);
        scheduleReconnect();
    }
}

function disconnect() {
    if (socket) {
        socket.close();
        socket = null;
    }
    state.connected = false;
    if (state.keepAliveTimer) clearInterval(state.keepAliveTimer);
}

function scheduleReconnect() {
    if (state.reconnectTimer) clearTimeout(state.reconnectTimer);
    state.reconnectTimer = setTimeout(() => {
        if (!state.connected) {
            console.log('[KrackedVoice] Attempting reconnection...');
            connectToVoiceServer();
        }
    }, 5000);
}

// ==================== Packet Management ====================

function sendAuthPacket() {
    const nameBytes = new TextEncoder().encode(state.playerName);
    const buffer = new ArrayBuffer(1 + 16 + 4 + nameBytes.length);
    const view = new DataView(buffer);
    let offset = 0;
    
    view.setUint8(offset++, PacketId.AUTHENTICATE);
    
    // UUID
    const hex = state.playerId.replace(/-/g, '');
    for (let i = 0; i < 16; i++) view.setUint8(offset++, parseInt(hex.substr(i * 2, 2), 16));
    
    // Name
    view.setUint32(offset, nameBytes.length); offset += 4;
    new Uint8Array(buffer, offset).set(nameBytes);
    
    sendEncrypted(buffer);
}

function sendMicPacket(pcmData) {
    const encoded = Hytale.audio.opusEncode(pcmData, CONFIG.sampleRate, CONFIG.channels);
    if (!encoded) return;
    
    const buffer = new ArrayBuffer(1 + 8 + 1 + 4 + encoded.length);
    const view = new DataView(buffer);
    let offset = 0;
    
    view.setUint8(offset++, PacketId.MIC);
    view.setBigUint64(offset, BigInt(state.sequenceNumber++)); offset += 8;
    view.setUint8(offset++, 0); // Not whispering
    view.setUint32(offset, encoded.length); offset += 4;
    new Uint8Array(buffer, offset).set(new Uint8Array(encoded));
    
    sendEncrypted(buffer);
}

function sendEncrypted(buffer) {
    // FIXED: Allow authentication even with default key for initial connection
    // The sync packet will update the key, then we re-authenticate
    if (state.secretKey === "KRACKED_DEFAULT_KEY") {
        console.warn('[KrackedVoice] Using default key - waiting for server sync...');
        // Still send the packet unencrypted for initial connection attempt
        if (socket) socket.send(new Uint8Array(buffer));
        return;
    }

    try {
        const encrypted = Hytale.crypto.aesEncrypt(new Uint8Array(buffer), state.secretKey);
        if (socket) socket.send(encrypted);
    } catch (e) {
        console.warn('[KrackedVoice] Encryption failed. Check Secret Key.', e);
        // Fallback: send unencrypted
        if (socket) socket.send(new Uint8Array(buffer));
    }
}

function handlePacket(data) {
    // FIXED: Handle both encrypted and unencrypted packets
    let decrypted;
    let wasEncrypted = true;

    try {
        // First try to decrypt
        decrypted = Hytale.crypto.aesDecrypt(new Uint8Array(data), state.secretKey);
    } catch (e) {
        // If decryption fails, try treating as unencrypted
        wasEncrypted = false;
        decrypted = new Uint8Array(data);
    }

    if (!decrypted || decrypted.length === 0) {
        console.warn('[KrackedVoice] Received empty packet');
        return;
    }

    const view = new DataView(decrypted.buffer);
    const packetId = view.getUint8(0);

    console.log(`[KrackedVoice] Received packet ${packetId} (${wasEncrypted ? 'encrypted' : 'unencrypted'})`);

    switch (packetId) {
        case PacketId.AUTHENTICATE_ACK:
            state.connected = true;
            console.log('%c[KrackedVoice] Authenticated Successfully! 🎙️', 'color: #00ff00; font-weight: bold;');
            Hytale.ui.chat.pushMessage("§d[KrackedVoice] §aConnected to voice server!");
            updateHud('connected');
            startKeepAlive();
            break;

        case PacketId.PLAYER_SOUND:
            handlePlayerSound(decrypted.buffer);
            break;

        case PacketId.PING:
            if (socket) socket.send(data);
            break;

        case PacketId.TOGGLE_GUI:
            if (Hytale.ui && Hytale.ui.toggle) {
                Hytale.ui.toggle('voice_settings');
            } else {
                console.warn('[KrackedVoice] UI toggle not available');
            }
            break;

        case PacketId.MUTE_STATE:
            const muted = view.getUint8(1) === 1;
            state.muted = muted;
            console.log(`[KrackedVoice] Mute state: ${muted}`);
            break;

        default:
            console.warn('[KrackedVoice] Unknown packet ID:', packetId);
    }
}

// ==================== Audio Output ====================

function handlePlayerSound(buffer) {
    if (state.deafened) return;
    const view = new DataView(buffer);
    let offset = 1;
    
    // Read UUID (16 bytes)
    const h = (n) => n.toString(16).padStart(16, '0');
    const most = view.getBigUint64(offset);
    const least = view.getBigUint64(offset + 8);
    const full = h(most) + h(least);
    const speakerId = `${full.slice(0,8)}-${full.slice(8,12)}-${full.slice(12,16)}-${full.slice(16,20)}-${full.slice(20)}`;
    offset += 16;

    const seq = view.getBigUint64(offset); offset += 8;
    const distance = view.getFloat32(offset); offset += 4;
    const isWhispering = view.getUint8(offset++) === 1;
    const dataLength = view.getUint32(offset); offset += 4;
    const opusData = new Uint8Array(buffer, offset, dataLength);
    
    const pcm = Hytale.audio.opusDecode(opusData, CONFIG.sampleRate, CONFIG.channels);
    playAudio(speakerId, pcm, distance);
}

function playAudio(speakerId, pcm, distance) {
    let player = audioPlayers.get(speakerId);
    if (!player) {
        const gain = audioContext.createGain();
        gain.connect(audioContext.destination);
        player = { gain, play: (data) => {
            const b = audioContext.createBuffer(1, data.length, CONFIG.sampleRate);
            b.getChannelData(0).set(data);
            const s = audioContext.createBufferSource();
            s.buffer = b; s.connect(gain); s.start();
        }};
        audioPlayers.set(speakerId, player);
    }
    
    // Proximity logic
    const vol = Math.max(0, 1.0 - (distance / CONFIG.proximityDistance)) * state.outputVolume;
    player.gain.gain.value = vol;
    player.play(pcm);
}

// ==================== Utilities ====================

function startKeepAlive() {
    if (state.keepAliveTimer) clearInterval(state.keepAliveTimer);
    state.keepAliveTimer = setInterval(() => {
        if (state.connected) {
            const buffer = new ArrayBuffer(1);
            new DataView(buffer).setUint8(0, PacketId.KEEP_ALIVE);
            sendEncrypted(buffer);
        }
    }, 5000);
}

function updateHud(status) {
    const hud = Hytale.ui.get('voice_hud');
    if (!hud) return;
    hud.setText('status_text', status === 'connected' ? 'KrackedVoice: Connected' : 'KrackedVoice: Offline');
}

// ==================== Robust Sync Logic ====================

function interceptSync(message) {
    if (!message) return false;

    let text = "";
    if (typeof message === 'string') {
        text = message;
    } else {
        // Try all known properties for chat messages in various Hytale API versions
        text = message.text || message.content || message.getText?.() || message.getRaw?.() || message.raw || "";
    }

    if (!text) return false;

    // Format 1: [KRACKED_SYNC]|||port|||key
    if (text.indexOf('[KRACKED_SYNC]') !== -1) {
        console.log('%c[KrackedVoice] %cSync packet detected (Format 1)!', 'color: #8a2be2; font-weight: bold;', 'color: #00ff00;');

        const cleanText = text.replace(/§[0-9a-fk-or]/g, '');
        const parts = cleanText.split(CONFIG.syncDelimiter);

        if (parts.length >= 3) {
            const port = parseInt(parts[1]);
            const secret = parts[2];

            if (!isNaN(port)) {
                applySyncConfig(port, secret);
            }
        }
        return true;
    }

    // Format 2: [KRACKED_JSON]{"mod":"kracked_voice","port":3012,"key":"..."}
    if (text.indexOf('[KRACKED_JSON]') !== -1) {
        console.log('%c[KrackedVoice] %cSync packet detected (Format 2 - JSON)!', 'color: #8a2be2; font-weight: bold;', 'color: #00ff00;');

        try {
            const jsonStr = text.replace('[KRACKED_JSON]', '').replace(/§[0-9a-fk-or]/g, '');
            const data = JSON.parse(jsonStr);

            if (data.mod === 'kracked_voice' && data.port) {
                applySyncConfig(data.port, data.key || state.secretKey);
            }
        } catch (e) {
            console.warn('[KrackedVoice] Failed to parse JSON sync:', e);
        }
        return true;
    }

    // Format 3: [KRACKED] 3012 (simple format)
    if (text.indexOf('[KRACKED]') !== -1) {
        console.log('%c[KrackedVoice] %cSync packet detected (Format 3 - Simple)!', 'color: #8a2be2; font-weight: bold;', 'color: #00ff00;');

        const cleanText = text.replace(/§[0-9a-fk-or]/g, '');
        const match = cleanText.match(/\[KRACKED\]\s*(\d+)/);

        if (match) {
            const port = parseInt(match[1]);
            applySyncConfig(port, state.secretKey);
        }
        return true;
    }

    return false;
}

/**
 * Apply sync configuration and reconnect if needed
 */
function applySyncConfig(port, secret) {
    const needsReconnect = (port !== CONFIG.voicePort || (secret && secret !== state.secretKey));

    CONFIG.voicePort = port;
    if (secret) state.secretKey = secret;

    console.log(`[KrackedVoice] Updated Config -> Port: ${port}, Key: ${secret ? secret.substring(0, 10) + '...' : 'unchanged'}`);

    if (needsReconnect) {
        if (socket) {
            console.log('[KrackedVoice] Restarting UDP Bridge with new config...');
            socket.close();
            socket = null;
        }
        connectToVoiceServer();
    } else if (state.connected === false) {
        console.log('[KrackedVoice] Sync updated, re-authenticating...');
        sendAuthPacket();
    }
}

// ==================== Mod Registration ====================
// Try multiple registration methods for maximum compatibility
if (typeof Hytale !== 'undefined' && Hytale.mods && Hytale.mods.register) {
    Hytale.mods.register('kracked_voice', {
        onPlayerJoinServer,
        onPlayerLeaveServer,
        onKeyDown: (key) => { if (key === 'V') state.talking = true; },
        onKeyUp: (key) => { if (key === 'V') state.talking = false; },

        voice: {
            muted: { get: () => state.muted, set: (v) => state.muted = v },
            deafened: { get: () => state.deafened, set: (v) => state.deafened = v },
            outputVolume: { get: () => state.outputVolume * 100, set: (v) => state.outputVolume = v / 100 }
        },

        // Standard mod hooks
        onChat: (m) => interceptSync(m),
        onChatMessage: (m) => interceptSync(m),
        onChatReceived: (m) => interceptSync(m),
        onMessage: (m) => interceptSync(m)
    });
    console.log('[KrackedVoice] Registered via Hytale.mods.register');
} else if (typeof registerMod !== 'undefined') {
    registerMod('kracked_voice', {
        onPlayerJoinServer,
        onPlayerLeaveServer,
        onChat: (m) => interceptSync(m)
    });
    console.log('[KrackedVoice] Registered via registerMod');
} else {
    console.warn('[KrackedVoice] No mod registration API found!');
}

// Event fallback for ultra-robust sync
if (typeof Hytale !== 'undefined' && Hytale.events) {
    const events = ['chat', 'chatMessage', 'messageReceived', 'onChat', 'onChatMessage', 'onChatReceived', 'message'];
    events.forEach(evt => {
        try {
            Hytale.events.on(evt, (e) => {
                if (!e) return;
                const message = e.message || e;
                if (interceptSync(message)) {
                    // Try all ways to cancel the event
                    if (e.cancel) e.cancel();
                    if (e.preventDefault) e.preventDefault();
                    if (e.stopImmediatePropagation) e.stopImmediatePropagation();
                    return true;
                }
            });
        } catch (ignored) {}
    });
}

// Global window flag for debug verification
window.KRACKED_VOICE_LOADED = true;
console.log('%c[KrackedVoice] %cPremium Client Bridge Loaded & Shielded.', 'color: #8a2be2; font-weight: bold;', 'color: #fff;');
