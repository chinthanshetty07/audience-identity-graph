package in.chinthanshetty.audience;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Keyed hashing (HMAC-SHA256) of normalised PII. Raw emails and phones never enter the identity graph; plain
 * SHA-256 isn't enough because phone numbers are guessable by brute force, so the hash needs a secret key.
 */
public final class Pii {
    private final SecretKeySpec key;

    public Pii(byte[] secret) {
        if (secret.length < 16) throw new IllegalArgumentException("HMAC key must be at least 16 bytes");
        this.key = new SecretKeySpec(secret.clone(), "HmacSHA256");
    }

    public String hash(String normalised) {
        if (normalised == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(normalised.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
