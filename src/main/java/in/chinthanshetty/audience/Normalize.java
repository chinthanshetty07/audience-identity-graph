package in.chinthanshetty.audience;

import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Puts PII into one canonical form before hashing, so "Priya.Rao+news@Gmail.com" and "priyarao@gmail.com" match. */
public final class Normalize {
    private static final Set<String> TITLES = Set.of("mr", "mrs", "ms", "miss", "dr", "shri", "smt", "kumari");
    private static final Set<String> ADDRESS_NOISE = Set.of("no", "flat", "apt", "apartment", "apartments", "road",
            "rd", "main", "cross", "street", "st", "layout", "nagar", "near", "opp", "bengaluru", "bangalore");

    private Normalize() {}

    public static String email(String raw) {
        if (raw == null || !raw.contains("@")) return null;
        String e = raw.trim().toLowerCase(Locale.ROOT);
        String local = e.substring(0, e.indexOf('@')), domain = e.substring(e.indexOf('@') + 1);
        if (domain.equals("googlemail.com")) domain = "gmail.com";
        if (domain.equals("gmail.com")) {                       // Gmail ignores dots and +tags
            local = local.replace(".", "");
            int plus = local.indexOf('+');
            if (plus >= 0) local = local.substring(0, plus);
        }
        return local.isEmpty() ? null : local + "@" + domain;
    }

    /** Indian mobile numbers to +91XXXXXXXXXX; anything that isn't a valid 10-digit mobile is dropped. */
    public static String phone(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("\\D", "");
        if (d.length() == 12 && d.startsWith("91")) d = d.substring(2);
        if (d.length() == 11 && d.startsWith("0")) d = d.substring(1);
        if (d.length() != 10 || "6789".indexOf(d.charAt(0)) < 0) return null;
        return "+91" + d;
    }

    public static String name(String raw) {
        if (raw == null) return "";
        String s = java.text.Normalizer.normalize(raw, Form.NFKD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", " ");
        return Arrays.stream(s.split("\\s+")).filter(t -> !t.isBlank() && !TITLES.contains(t))
                .collect(Collectors.joining(" "));
    }

    private static final java.util.Map<String, String> ABBREV = java.util.Map.of(
            "crs", "cross", "x", "cross", "mn", "main", "rd", "road", "st", "street", "blk", "block");

    /**
     * Address tokens for fuzzy comparison. Abbreviations are expanded, and an ordinal is glued to the word after
     * it, so "12th Cross, 5th Main" gives {12th-cross, 5th-main} rather than {12th, 5th}: otherwise "12th Cross"
     * and "12th Main" look identical and different households get merged.
     */
    public static Set<String> addressTokens(String raw) {
        if (raw == null) return Set.of();
        String[] t = Arrays.stream(raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(x -> !x.isBlank()).map(x -> ABBREV.getOrDefault(x, x)).toArray(String[]::new);
        Set<String> out = new java.util.TreeSet<>();
        for (int i = 0; i < t.length; i++) {
            if (t[i].matches("\\d+(st|nd|rd|th)?") && i + 1 < t.length && Set.of("cross", "main", "block", "stage", "phase").contains(t[i + 1])) {
                out.add(t[i].replaceAll("\\D", "") + "-" + t[++i]);
            } else if (t[i].length() > 1 && !ADDRESS_NOISE.contains(t[i])) {
                out.add(t[i]);
            }
        }
        return out;
    }
}
