package in.chinthanshetty.audience;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AudienceTest {
    static Identity id(int n, boolean consent, String interest) {
        return new Identity("TU-" + n, List.of("bank:" + n), Set.of("h" + n), Set.of(), consent, Map.of("interest", Set.of(interest)));
    }

    @Test
    void smallAudiencesAreSuppressed() {
        List<Identity> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) ids.add(id(i, true, "travel"));
        var r = new Audience(25).build("tiny", ids, i -> i.has("interest", "travel"));
        assertTrue(r.suppressed());
        assertEquals(0, r.size());
        assertTrue(r.hashedEmails().isEmpty(), "a suppressed audience must not leak members");
    }

    @Test
    void optedOutIdentitiesNeverAppear() {
        List<Identity> ids = new ArrayList<>();
        for (int i = 0; i < 30; i++) ids.add(id(i, i % 3 != 0, "travel"));
        var r = new Audience(5).build("travel", ids, i -> i.has("interest", "travel"));
        assertEquals(20, r.size());
        assertFalse(r.hashedEmails().contains("h0"));
    }

    @Test
    void optOutsDoNotCountTowardsK() {
        List<Identity> ids = new ArrayList<>();
        for (int i = 0; i < 30; i++) ids.add(id(i, i < 20, "travel"));
        assertTrue(new Audience(25).build("t", ids, i -> true).suppressed());
    }

    @Test
    void kMustBeMeaningful() {
        assertThrows(IllegalArgumentException.class, () -> new Audience(1));
    }
}
