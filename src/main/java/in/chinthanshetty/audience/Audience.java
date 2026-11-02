package in.chinthanshetty.audience;

import java.util.List;
import java.util.function.Predicate;

/**
 * Builds a marketing audience from resolved identities, with two privacy rules enforced in code:
 * opted-out identities are never included, and an audience smaller than k is suppressed, so a narrow
 * segment ("pincode X, age 70+, interest Y") can't be used to single out a few people.
 */
public final class Audience {
    public record Result(String name, boolean suppressed, int size, List<String> hashedEmails) {}

    private final int minSize;

    public Audience(int minSize) {
        if (minSize < 2) throw new IllegalArgumentException("k must be at least 2");
        this.minSize = minSize;
    }

    public Result build(String name, List<Identity> identities, Predicate<Identity> rule) {
        List<Identity> members = identities.stream().filter(Identity::consent).filter(rule).toList();
        if (members.size() < minSize) return new Result(name, true, 0, List.of());
        List<String> emails = members.stream().flatMap(i -> i.emailHashes().stream().limit(1)).toList();
        return new Result(name, false, members.size(), emails);
    }
}
