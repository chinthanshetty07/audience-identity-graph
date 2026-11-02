package in.chinthanshetty.audience;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PiiTest {
    @Test
    void keyedHashIsStableAndKeyDependent() {
        Pii a = new Pii("key-one-0123456789".getBytes(StandardCharsets.UTF_8));
        Pii b = new Pii("key-two-0123456789".getBytes(StandardCharsets.UTF_8));
        assertEquals(a.hash("+919876543210"), a.hash("+919876543210"));
        assertNotEquals(a.hash("+919876543210"), b.hash("+919876543210"));
        assertEquals(64, a.hash("x").length());
        assertNull(a.hash(null));
    }

    @Test
    void rejectsShortKeys() {
        assertThrows(IllegalArgumentException.class, () -> new Pii("short".getBytes(StandardCharsets.UTF_8)));
    }
}
