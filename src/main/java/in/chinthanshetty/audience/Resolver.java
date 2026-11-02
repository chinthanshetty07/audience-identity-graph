package in.chinthanshetty.audience;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Identity resolution: links records that belong to the same person.
 *
 * <ol>
 *   <li>Deterministic: same hashed email, or same hashed mobile number.</li>
 *   <li>Probabilistic, within the same pincode only (blocking keeps it from being n²): similar names
 *       (Jaro-Winkler) and overlapping address tokens.</li>
 * </ol>
 * The classic failure is over-linking: one office landline or a "9999999999" placeholder on hundreds of
 * forms merges strangers into a single giant identity. Identifiers shared by too many different names are
 * treated as non-identifying and ignored.
 */
public final class Resolver {
    public record Config(double nameThreshold, double addressThreshold, int hubThreshold) {
        public static Config defaults() { return new Config(0.92, 0.6, 4); }
    }

    private record Hashed(SourceRecord rec, String email, String phone, String name, Set<String> address) {}

    private final Pii pii;
    private final Config cfg;
    private final List<String> ignoredHubs = new ArrayList<>();

    public Resolver(Pii pii, Config cfg) {
        this.pii = pii;
        this.cfg = cfg;
    }

    public List<String> ignoredHubs() { return List.copyOf(ignoredHubs); }

    public List<Identity> resolve(List<SourceRecord> records) {
        List<Hashed> hs = records.stream().map(r -> new Hashed(r, pii.hash(Normalize.email(r.email())),
                pii.hash(Normalize.phone(r.phone())), Normalize.name(r.name()), Normalize.addressTokens(r.address()))).toList();
        Set<String> hubs = hubs(hs);
        UnionFind uf = new UnionFind(hs.size());

        Map<String, Integer> firstByKey = new HashMap<>();
        for (int i = 0; i < hs.size(); i++) {
            Hashed h = hs.get(i);
            for (String key : new String[] {h.email() == null ? null : "e:" + h.email(), h.phone() == null ? null : "p:" + h.phone()}) {
                if (key == null || hubs.contains(key)) continue;
                Integer first = firstByKey.putIfAbsent(key, i);
                if (first != null) uf.union(first, i);
            }
        }

        Map<String, List<Integer>> blocks = new HashMap<>();
        for (int i = 0; i < hs.size(); i++) {
            String pin = hs.get(i).rec().pincode();
            if (pin != null && !pin.isBlank()) blocks.computeIfAbsent(pin.trim(), k -> new ArrayList<>()).add(i);
        }
        for (List<Integer> block : blocks.values()) {
            for (int a = 0; a < block.size(); a++) {
                for (int b = a + 1; b < block.size(); b++) {
                    Hashed x = hs.get(block.get(a)), y = hs.get(block.get(b));
                    if (!conflicting(x, y)
                            && JaroWinkler.similarity(x.name(), y.name()) >= cfg.nameThreshold()
                            && jaccard(x.address(), y.address()) >= cfg.addressThreshold()) {
                        uf.union(block.get(a), block.get(b));
                    }
                }
            }
        }

        Map<Integer, List<Hashed>> clusters = new TreeMap<>();
        for (int i = 0; i < hs.size(); i++) clusters.computeIfAbsent(uf.find(i), k -> new ArrayList<>()).add(hs.get(i));
        List<Identity> out = new ArrayList<>();
        for (List<Hashed> c : clusters.values()) out.add(toIdentity(c, hubs));
        return out;
    }

    private Set<String> hubs(List<Hashed> hs) {
        Map<String, Set<String>> namesByKey = new HashMap<>();
        for (Hashed h : hs) {
            if (h.email() != null) namesByKey.computeIfAbsent("e:" + h.email(), k -> new HashSet<>()).add(h.name());
            if (h.phone() != null) namesByKey.computeIfAbsent("p:" + h.phone(), k -> new HashSet<>()).add(h.name());
        }
        Set<String> hubs = new HashSet<>();
        namesByKey.forEach((k, names) -> {
            if (names.size() >= cfg.hubThreshold()) {
                hubs.add(k);
                ignoredHubs.add(k.substring(0, 2) + k.substring(2, 10) + "… shared by " + names.size() + " names");
            }
        });
        return hubs;
    }

    private Identity toIdentity(List<Hashed> c, Set<String> hubs) {
        List<String> ids = new ArrayList<>();
        Set<String> emails = new TreeSet<>(), phones = new TreeSet<>();
        Map<String, Set<String>> attrs = new TreeMap<>();
        boolean consent = true;
        for (Hashed h : c) {
            ids.add(h.rec().source() + ":" + h.rec().sourceId());
            if (h.email() != null && !hubs.contains("e:" + h.email())) emails.add(h.email());
            if (h.phone() != null && !hubs.contains("p:" + h.phone())) phones.add(h.phone());
            consent &= h.rec().consent();
            h.rec().attributes().forEach((k, v) -> attrs.computeIfAbsent(k, x -> new TreeSet<>()).add(v));
        }
        String id = "TU-" + pii.hash(String.join("|", new TreeSet<>(ids))).substring(0, 12);
        return new Identity(id, ids, emails, phones, consent, attrs);
    }

    /** Two records with different emails AND different mobiles are different people, however alike their names. */
    private static boolean conflicting(Hashed x, Hashed y) {
        return x.email() != null && y.email() != null && !x.email().equals(y.email())
                && x.phone() != null && y.phone() != null && !x.phone().equals(y.phone());
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / union.size();
    }

    static final class UnionFind {
        private final int[] parent;

        UnionFind(int n) {
            parent = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
        }

        int find(int x) {
            while (parent[x] != x) x = parent[x] = parent[parent[x]];
            return x;
        }

        void union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra != rb) parent[Math.max(ra, rb)] = Math.min(ra, rb);
        }
    }
}
