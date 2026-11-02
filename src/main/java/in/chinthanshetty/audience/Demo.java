package in.chinthanshetty.audience;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Resolves 400 synthetic people's records from three partners, scores the linking and builds two audiences. */
public final class Demo {
    public static void main(String[] args) {
        String secret = System.getenv().getOrDefault("PII_HMAC_KEY", "demo-only-key-not-a-secret-123");
        Pii pii = new Pii(secret.getBytes(StandardCharsets.UTF_8));
        List<Synthetic.Labelled> data = Synthetic.generate(400, 42);
        Resolver resolver = new Resolver(pii, Resolver.Config.defaults());
        List<Identity> ids = resolver.resolve(data.stream().map(Synthetic.Labelled::record).toList());
        Evaluation.Scores s = Evaluation.score(data, ids);
        System.out.printf("%d records -> %d identities (400 real people)%n", data.size(), ids.size());
        System.out.printf("pairwise precision %.3f, recall %.3f (%d of %d true pairs linked)%n",
                s.precision(), s.recall(), s.correctPairs(), s.truePairs());
        resolver.ignoredHubs().forEach(h -> System.out.println("ignored shared identifier: " + h));

        Audience audience = new Audience(25);
        for (var r : List.of(
                audience.build("Bengaluru travel lovers 25-34", ids, i -> i.has("interest", "travel") && i.has("age_band", "25-34")),
                audience.build("Whitefield 45-54 gamers", ids, i -> i.has("interest", "gaming") && i.has("age_band", "45-54")
                        && i.sourceIds().stream().anyMatch(x -> x.startsWith("bank"))))) {
            System.out.println(r.suppressed() ? r.name() + ": suppressed (below k=25)"
                    : r.name() + ": " + r.size() + " consenting identities, exported as hashed emails");
        }
        long optedOut = ids.stream().filter(i -> !i.consent()).count();
        System.out.println(optedOut + " identities excluded from every audience because a partner holds an opt-out");
    }
}
