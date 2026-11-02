package in.chinthanshetty.audience;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One resolved person. Holds only hashed identifiers, never raw PII. Consent is the strictest of its records:
 * if any linked record opted out, the identity is out of every audience.
 */
public record Identity(String id, List<String> sourceIds, Set<String> emailHashes, Set<String> phoneHashes,
                       boolean consent, Map<String, Set<String>> attributes) {

    public boolean has(String attribute, String value) {
        return attributes.getOrDefault(attribute, Set.of()).contains(value);
    }
}
