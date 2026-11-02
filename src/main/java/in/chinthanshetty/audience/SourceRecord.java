package in.chinthanshetty.audience;

import java.util.Map;

/** One customer record as a data partner sends it: raw PII, a consent flag and some marketing attributes. */
public record SourceRecord(String source, String sourceId, String name, String email, String phone,
                           String address, String pincode, boolean consent, Map<String, String> attributes) {}
