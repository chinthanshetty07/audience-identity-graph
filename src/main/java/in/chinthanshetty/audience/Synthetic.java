package in.chinthanshetty.audience;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates made-up people and the messy records several data partners would hold about them: different email
 * spellings (dots, +tags, case), phone formats, typos in names, abbreviated addresses, missing fields. Every
 * record carries the id of the person it really belongs to, so linking quality can be measured.
 */
public final class Synthetic {
    public record Labelled(SourceRecord record, int personId) {}

    private static final String[] FIRST = {"Priya", "Rohit", "Sneha", "Arjun", "Kavya", "Vikram", "Divya", "Karthik",
            "Neha", "Suresh", "Asha", "Manoj", "Ananya", "Rahul", "Meera", "Aditya"};
    private static final String[] LAST = {"Rao", "Sharma", "Iyer", "Reddy", "Nair", "Gowda", "Menon", "Kulkarni",
            "Shetty", "Das", "Pillai", "Joshi"};
    private static final String[] AREAS = {"Koramangala", "Indiranagar", "Whitefield", "Jayanagar", "HSR Layout", "Malleshwaram"};
    private static final String[] PINS = {"560034", "560038", "560066", "560041", "560102", "560003"};
    private static final String[] INTERESTS = {"travel", "cricket", "fitness", "investing", "cooking", "gaming"};
    private static final String[] SOURCES = {"bank", "telco", "retailer"};

    private Synthetic() {}

    public static List<Labelled> generate(int people, long seed) {
        Random r = new Random(seed);
        List<Labelled> out = new ArrayList<>();
        for (int p = 0; p < people; p++) {
            String first = FIRST[r.nextInt(FIRST.length)], last = LAST[r.nextInt(LAST.length)];
            String user = (first + "." + last).toLowerCase() + (100 + p);
            String mobile = "9" + String.format("%09d", r.nextInt(1_000_000_000));
            int area = r.nextInt(AREAS.length);
            String street = (1 + r.nextInt(40)) + "th Cross, " + (1 + r.nextInt(12)) + "th Main, " + AREAS[area];
            boolean consent = r.nextDouble() > 0.1;
            String age = new String[] {"18-24", "25-34", "35-44", "45-54"}[r.nextInt(4)];
            int copies = 1 + r.nextInt(3);
            for (int c = 0; c < copies; c++) {
                String source = SOURCES[c % SOURCES.length];
                String name = c == 0 ? first + " " + last : variant(first, last, r);
                String email = switch (r.nextInt(4)) {
                    case 0 -> user + "@gmail.com";
                    case 1 -> user.replace(".", "") + "+" + source + "@gmail.com";
                    case 2 -> user.toUpperCase() + "@GMail.com";
                    default -> r.nextBoolean() ? null : user + "@gmail.com";
                };
                String phone = switch (r.nextInt(4)) {
                    case 0 -> mobile;
                    case 1 -> "+91 " + mobile.substring(0, 5) + " " + mobile.substring(5);
                    case 2 -> "0" + mobile;
                    default -> null;
                };
                String address = r.nextBoolean() ? "#" + street : street.replace("Cross", "Crs").replace("Main", "Mn");
                Map<String, String> attrs = new HashMap<>();
                attrs.put("age_band", age);
                attrs.put("city", "Bengaluru");
                attrs.put("interest", INTERESTS[r.nextInt(INTERESTS.length)]);
                // One partner holds an opt-out the others don't know about.
                boolean recConsent = consent || c > 0;
                out.add(new Labelled(new SourceRecord(source, source + "-" + p + "-" + c, name, email, phone, address,
                        PINS[area], recConsent, attrs), p));
            }
        }
        // Placeholder phone typed by a call centre on many unrelated forms: should not merge those people.
        for (int i = 0; i < 6; i++) {
            Labelled victim = out.get(r.nextInt(out.size()));
            SourceRecord v = victim.record();
            out.add(new Labelled(new SourceRecord("callcentre", "cc-" + i, v.name(), null, "9999999999", v.address(),
                    v.pincode(), v.consent(), v.attributes()), victim.personId()));
        }
        return out;
    }

    private static String variant(String first, String last, Random r) {
        return switch (r.nextInt(4)) {
            case 0 -> "Mr. " + first + " " + last;
            case 1 -> first.toUpperCase() + " " + last.toUpperCase();
            case 2 -> first + first.charAt(first.length() - 1) + " " + last;     // one-letter typo
            default -> first + " " + last;
        };
    }
}
