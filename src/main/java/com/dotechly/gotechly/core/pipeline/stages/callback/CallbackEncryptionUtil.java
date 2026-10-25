package com.dotechly.gotechly.core.pipeline.stages.callback;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts callback credentials for the DFS/Huawei CPS SOAP request.
 *
 * Plaintext structure:
 *   [StructureVersion=01][Obfuscation=1234][PaddedLength][timestamp][credential][conversationID][ConversationSeq=01]
 *
 * Output: Base64( ciphertext+authTag || IV[16] )  — IV appended at end.
 *
 * The AES key must be Base64-encoded and stored in app_configs
 * under callback.<channelName>.encryption-key.
 */
public final class CallbackEncryptionUtil {

    private static final String STRUCTURE_VERSION    = "01";
    private static final String OBFUSCATION          = "1234";
    private static final String CONVERSATION_SEQ     = "01";

    private CallbackEncryptionUtil() {}

    /**
     * Encrypts a credential bound to the current transaction.
     *
     * @param credential     the raw secret (e.g. password or securityCredential)
     * @param transactionRef the conversation/transaction ID (e.g. requestId)
     * @param timestamp      the request timestamp as yyyyMMddHHmmss
     * @param base64Key      the AES key, Base64-encoded (16, 24, or 32 bytes)
     * @return Base64-encoded string: ciphertext+authTag || IV[16]
     * @throws CallbackRenderException if encryption fails
     */
    public static String encrypt(String credential, String transactionRef,
                                  String timestamp, String base64Key) {
        try {
            String plaintext = buildObfuscatedPlaintext(timestamp, transactionRef, credential);
            return aesDynamicIVEncrypt(plaintext, base64Key);
        } catch (Exception e) {
            throw new CallbackRenderException(
                    "AES/GCM encryption failed for callback credential: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the structured plaintext DFS expects:
     * [StructureVersion][Obfuscation][PaddedLength][timestamp][credential][conversationID][ConversationSeq]
     */
    private static String buildObfuscatedPlaintext(String timestamp, String conversationID,
                                                    String credential) {
        if (credential == null || credential.isEmpty()) {
            return null;
        }
        int len = credential.length();
        String paddedLen = (len > 9) ? String.valueOf(len) : "0" + len;

        return STRUCTURE_VERSION
                + OBFUSCATION
                + paddedLen
                + timestamp
                + credential
                + conversationID
                + CONVERSATION_SEQ;
    }

    /**
     * AES/GCM encrypt with a dynamic 16-byte IV appended at the end.
     * Output layout: Base64( ciphertext+authTag || IV[16] )
     */
    private static String aesDynamicIVEncrypt(String input, String key) throws Exception {
        byte[] byteKey   = Base64.getDecoder().decode(key);
        byte[] byteInput = input.getBytes(StandardCharsets.UTF_8);

        Cipher cipher = Cipher.getInstance("AES/GCM/NOPADDING");

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(byteKey, "AES"),
                new GCMParameterSpec(16 * Byte.SIZE, iv));

        byte[] cipherBytes = cipher.doFinal(byteInput);

        // ciphertext+authTag first, IV at the end
        byte[] output = new byte[cipherBytes.length + 16];
        System.arraycopy(cipherBytes, 0, output, 0,              cipherBytes.length);
        System.arraycopy(iv,          0, output, cipherBytes.length, iv.length);

        return Base64.getEncoder().encodeToString(output);
    }
}
