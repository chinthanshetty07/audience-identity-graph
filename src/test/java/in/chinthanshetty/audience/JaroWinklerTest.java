package in.chinthanshetty.audience;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JaroWinklerTest {
    @Test
    void knownValues() {
        assertEquals(1.0, JaroWinkler.similarity("priya", "priya"));
        assertEquals(0.961, JaroWinkler.similarity("martha", "marhta"), 0.001);   // textbook example
        assertTrue(JaroWinkler.similarity("kavya nair", "kavyaa nair") > 0.95);
        assertTrue(JaroWinkler.similarity("arjun gowda", "suresh pillai") < 0.6);
        assertEquals(0.0, JaroWinkler.similarity("", "x"));
    }
}
