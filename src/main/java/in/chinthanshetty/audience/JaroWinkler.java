package in.chinthanshetty.audience;

/** Jaro-Winkler similarity in [0, 1]; good for short strings like names with typos ("Priya" vs "Priyaa"). */
public final class JaroWinkler {
    private JaroWinkler() {}

    public static double similarity(String a, String b) {
        if (a.equals(b)) return a.isEmpty() ? 0 : 1;
        if (a.isEmpty() || b.isEmpty()) return 0;
        int range = Math.max(0, Math.max(a.length(), b.length()) / 2 - 1);
        boolean[] ma = new boolean[a.length()], mb = new boolean[b.length()];
        int matches = 0;
        for (int i = 0; i < a.length(); i++) {
            for (int j = Math.max(0, i - range); j < Math.min(b.length(), i + range + 1); j++) {
                if (!mb[j] && a.charAt(i) == b.charAt(j)) {
                    ma[i] = mb[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) return 0;
        int t = 0, k = 0;
        for (int i = 0; i < a.length(); i++) {
            if (!ma[i]) continue;
            while (!mb[k]) k++;
            if (a.charAt(i) != b.charAt(k++)) t++;
        }
        double m = matches, jaro = (m / a.length() + m / b.length() + (m - t / 2.0) / m) / 3;
        int prefix = 0;
        while (prefix < Math.min(4, Math.min(a.length(), b.length())) && a.charAt(prefix) == b.charAt(prefix)) prefix++;
        return jaro + prefix * 0.1 * (1 - jaro);
    }
}
