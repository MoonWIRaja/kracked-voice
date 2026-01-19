package dev.kracked.voice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for voice data
 */
public class AesEncryption {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AesEncryption.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    private final SecretKeySpec secretKey;
    private final SecureRandom random;
    
    public AesEncryption(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        
        // Ensure 256-bit key
        if (keyBytes.length != 32) {
            byte[] adjustedKey = new byte[32];
            System.arraycopy(keyBytes, 0, adjustedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = adjustedKey;
        }
        
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.random = new SecureRandom();
    }
    
    /**
     * Encrypt data with AES-256-GCM
     * @param plaintext Data to encrypt
     * @return IV + ciphertext
     */
    public byte[] encrypt(byte[] plaintext) {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // Prepend IV to ciphertext
            byte[] result = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, result, GCM_IV_LENGTH, ciphertext.length);
            
            return result;
        } catch (Exception e) {
            LOGGER.error("Encryption failed", e);
            return null;
        }
    }
    
    /**
     * Decrypt data with AES-256-GCM
     * @param ciphertext IV + encrypted data
     * @return Decrypted data
     */
    public byte[] decrypt(byte[] ciphertext) {
        try {
            if (ciphertext.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Ciphertext too short");
            }
            
            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(ciphertext, 0, iv, 0, GCM_IV_LENGTH);
            
            // Extract encrypted data
            byte[] encrypted = new byte[ciphertext.length - GCM_IV_LENGTH];
            System.arraycopy(ciphertext, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            LOGGER.error("Decryption failed", e);
            return null;
        }
    }
}
