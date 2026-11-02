package in.chinthanshetty.audience;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class NormalizeTest {
    @Test
    void gmailDotsTagsAndCaseCollapse() {
        assertEquals("priyarao@gmail.com", Normalize.email("Priya.Rao+news@GMail.com"));
        assertEquals("priyarao@gmail.com", Normalize.email("priya.rao@googlemail.com"));
        assertEquals("priya.rao@company.in", Normalize.email("Priya.Rao@Company.in"));   // dots matter elsewhere
        assertNull(Normalize.email("not-an-email"));
    }

    @Test
    void indianMobilesNormaliseAndJunkIsDropped() {
        assertEquals("+919876543210", Normalize.phone("098765 43210"));
        assertEquals("+919876543210", Normalize.phone("+91 98765-43210"));
        assertNull(Normalize.phone("2544 1722"));          // landline without STD code
        assertNull(Normalize.phone("5876543210"));         // Indian mobiles start with 6-9
        // Known limitation: "080 2544 1722" (Bengaluru landline) is indistinguishable by digits from a
        // trunk-prefixed mobile, so it normalises to +918025441722. Documented in the README.
        assertEquals("+918025441722", Normalize.phone("080 2544 1722"));
        assertNull(Normalize.phone("12345"));
    }

    @Test
    void namesLoseTitlesAccentsAndCase() {
        assertEquals("priya rao", Normalize.name("Mrs. PRIYA Rao"));
        assertEquals("rene das", Normalize.name("René Das"));
    }

    @Test
    void addressKeepsCrossAndMainApart() {
        assertEquals(Set.of("12-cross", "5-main", "koramangala"), Normalize.addressTokens("#12th Crs, 5th Mn, Koramangala"));
        assertNotEquals(Normalize.addressTokens("12th Cross, Koramangala"), Normalize.addressTokens("12th Main, Koramangala"));
    }
}
