package in.chinthanshetty.audience;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResolverTest {
    static final Pii PII = new Pii("test-key-0123456789".getBytes(StandardCharsets.UTF_8));

    static SourceRecord rec(String src, String id, String name, String email, String phone, String addr, String pin, boolean consent) {
        return new SourceRecord(src, id, name, email, phone, addr, pin, consent, Map.of("interest", "travel"));
    }

    static List<Identity> resolve(SourceRecord... rs) {
        return new Resolver(PII, Resolver.Config.defaults()).resolve(List.of(rs));
    }

    @Test
    void sameEmailInDifferentSpellingsLinks() {
        var ids = resolve(rec("bank", "1", "Priya Rao", "Priya.Rao@gmail.com", null, null, null, true),
                rec("telco", "2", "P Rao", "priyarao+telco@gmail.com", null, null, null, true));
        assertEquals(1, ids.size());
    }

    @Test
    void sameMobileInDifferentFormatsLinks() {
        var ids = resolve(rec("bank", "1", "Rohit", null, "09876543210", null, null, true),
                rec("telco", "2", "Rohit S", null, "+91 98765 43210", null, null, true));
        assertEquals(1, ids.size());
    }

    @Test
    void fuzzyNameAndAddressLinkWithinPincode() {
        var ids = resolve(rec("bank", "1", "Kavya Nair", null, null, "#12th Cross, 5th Main, Jayanagar", "560041", true),
                rec("retailer", "2", "Kavyaa Nair", null, null, "12th Crs, 5th Mn, Jayanagar", "560041", true));
        assertEquals(1, ids.size());
    }

    @Test
    void sameNameAndAddressInDifferentPincodeDoesNotLink() {
        var ids = resolve(rec("bank", "1", "Kavya Nair", null, null, "12th Cross, 5th Main", "560041", true),
                rec("retailer", "2", "Kavya Nair", null, null, "12th Cross, 5th Main", "560034", true));
        assertEquals(2, ids.size());
    }

    @Test
    void neighboursWithTheSameNameStayApart() {
        var ids = resolve(rec("bank", "1", "Arjun Gowda", null, null, "14th Cross, 6th Main, Koramangala", "560034", true),
                rec("bank", "2", "Arjun Gowda", null, null, "14th Cross, 8th Main, Koramangala", "560034", true));
        assertEquals(2, ids.size(), "same cross, different main: different households");
    }

    @Test
    void conflictingEmailAndPhoneBlockFuzzyLinks() {
        var ids = resolve(rec("bank", "1", "Karthik Sharma", "k1@gmail.com", "9111111111", "14th Cross, 6th Main, Koramangala", "560034", true),
                rec("bank", "2", "Karthik Sharma", "k2@gmail.com", "9222222222", "14th Cross, 6th Main, Koramangala", "560034", true));
        assertEquals(2, ids.size());
    }

    @Test
    void placeholderPhoneSharedByManyNamesIsIgnored() {
        Resolver r = new Resolver(PII, Resolver.Config.defaults());
        var ids = r.resolve(List.of(
                rec("cc", "1", "Asha Iyer", null, "9999999999", null, null, true),
                rec("cc", "2", "Manoj Das", null, "9999999999", null, null, true),
                rec("cc", "3", "Neha Joshi", null, "9999999999", null, null, true),
                rec("cc", "4", "Suresh Pillai", null, "9999999999", null, null, true)));
        assertEquals(4, ids.size());
        assertEquals(1, r.ignoredHubs().size());
        assertTrue(ids.stream().allMatch(i -> i.phoneHashes().isEmpty()));
    }

    @Test
    void identitiesHoldOnlyHashesNeverRawPii() {
        var ids = resolve(rec("bank", "1", "Priya Rao", "priya@gmail.com", "9876543210", "addr", "560041", true));
        String dump = ids.toString();
        assertFalse(dump.contains("priya@gmail.com") || dump.contains("9876543210"));
    }

    @Test
    void anyOptOutWins() {
        var ids = resolve(rec("bank", "1", "Priya Rao", "priya@gmail.com", null, null, null, true),
                rec("telco", "2", "Priya Rao", "priya@gmail.com", null, null, null, false));
        assertFalse(ids.get(0).consent());
    }

    @Test
    void identityIdIsStableAcrossRunsAndInputOrder() {
        var a = rec("bank", "1", "Priya Rao", "priya@gmail.com", null, null, null, true);
        var b = rec("telco", "2", "Priya Rao", "priya@gmail.com", null, null, null, true);
        assertEquals(resolve(a, b).get(0).id(), resolve(b, a).get(0).id());
    }

    @Test
    void largerUnseenDatasetStaysAccurate() {
        var data = Synthetic.generate(1500, 7);
        var ids = new Resolver(PII, Resolver.Config.defaults()).resolve(data.stream().map(Synthetic.Labelled::record).toList());
        var s = Evaluation.score(data, ids);
        assertTrue(s.precision() >= 0.98, "precision " + s.precision());
        assertTrue(s.recall() >= 0.98, "recall " + s.recall());
    }
}
