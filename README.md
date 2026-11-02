# Audience Identity Graph

[![CI](https://github.com/chinthanshetty07/audience-identity-graph/actions/workflows/ci.yml/badge.svg)](https://github.com/chinthanshetty07/audience-identity-graph/actions/workflows/ci.yml)

Identity resolution and privacy-safe audience building in plain Java 21: take customer records from several
data partners, work out which ones belong to the same person, and build marketing audiences from the result
without exposing raw personal data, including people who opted out, or segments small enough to single
someone out.

Built as an independent learning project after applying to an engineering role on TransUnion's TruAudience
team. It is not affiliated with TransUnion, it is far simpler than a production identity graph, and **all
people and records are synthetic** (`Synthetic.java`).

## Pipeline

```
partner records -> normalise (email, mobile, name, address) -> HMAC-SHA256 hash PII -> drop raw PII
   -> deterministic links (same hashed email / mobile)
   -> probabilistic links within a pincode (Jaro-Winkler name + address-token overlap)
   -> union-find clusters = identities (hashes only; strictest consent wins)
   -> audiences: rule -> consent filter -> k-anonymity (suppress if < k) -> export hashed emails
```

| Concern | How it's handled |
|---|---|
| Messy identifiers | Gmail dots, `+tags` and case collapse (`Priya.Rao+news@GMail.com` = `priyarao@gmail.com`); `098765 43210`, `+91 98765-43210` and `9876543210` are the same mobile; titles, accents and case stripped from names; `Crs`/`Mn`/`Rd` expanded in addresses. |
| Raw PII | Emails and phones are hashed with a **keyed** HMAC before linking; identities only ever hold hashes. Plain SHA-256 isn't enough for phone numbers: there are only ~10^10 of them, so an unkeyed hash can be reversed by brute force. |
| Over-linking | An identifier shared by many different names (a call centre's `9999999999`, an office landline) is ignored, not used to merge strangers into one giant identity. |
| Namesakes | Two records whose emails **and** mobiles are both present and both different are never fuzzy-merged, however alike the names and addresses. |
| Consent | If any partner holds an opt-out for a person, the whole identity is excluded from every audience. Opt-outs don't count towards k. |
| Re-identification | An audience smaller than k (default 25) returns "suppressed" with no members, so narrow segments can't be used to identify individuals. |
| Stable ids | An identity's id is derived from its member records, so it's the same across runs and input order. |

## Results (`mvn -q compile exec:java`)

```
816 records -> 400 identities (400 real people)
pairwise precision 1.000, recall 1.000 (559 of 559 true pairs linked)
ignored shared identifier: p:b42a41d8… shared by 6 names
Bengaluru travel lovers 25-34: 34 consenting identities, exported as hashed emails
Whitefield 45-54 gamers: suppressed (below k=25)
27 identities excluded from every audience because a partner holds an opt-out
```

A test also runs a larger dataset with a different seed (1,500 people) and requires precision and recall of
at least 0.98, to check the thresholds aren't just tuned to the demo data.

### How the evaluation found two over-linking bugs

The first version scored **recall 1.000 but precision 0.861**: it was merging different people. Listing the
wrongly merged clusters showed two causes:

1. Address tokens lost structure: "12th Cross" and "12th Main" both became the token `12th`, so different
   houses looked identical. Fixed by gluing ordinals to their road type (`12-cross`, `5-main`) and expanding
   abbreviations. Precision went to 0.935.
2. Namesakes living on the same Main in the same area: "Karthik Sharma, 14th Cross, 10th Main" and "Karthik
   Sharma, 14th Cross, 6th Main" share 2 of 4 address tokens, exactly the 0.5 threshold. Fixed by raising the
   threshold to 0.6 and refusing fuzzy merges when both records carry different emails and different mobiles.
   Precision went to 1.000 with recall unchanged.

The synthetic data is cleaner than real partner data, so these numbers say the logic is right, not that it
would score 1.000 in production.

### A known limitation

A Bengaluru landline written `080 2544 1722` normalises to `+918025441722`, a valid-looking mobile, because
by digits alone it's indistinguishable from a trunk-prefixed mobile (`0` + 10 digits starting with 8). Real
pipelines resolve this with the partner's field type (landline vs mobile) or a numbering-plan lookup. A test
documents the current behaviour.

## Run

```bash
mvn verify                  # 22 tests
mvn -q compile exec:java    # demo
```

Set `PII_HMAC_KEY` to change the hashing key; the demo's default key is a placeholder, not a secret.

## What a real identity graph adds

- Billions of records: blocking and matching on Spark, incremental updates instead of full re-resolution, and
  identity ids that survive merges and splits.
- Learned match models with calibrated thresholds and human review of borderline links.
- Household and device graphs, recency and source-trust weighting, and conflict handling between partners.
- Key management for the HMAC secret, audit of every audience export, and region-specific consent rules (DPDP Act).

Built with Claude Code; the evaluation against ground truth and the 22 tests are how I checked what it wrote.
