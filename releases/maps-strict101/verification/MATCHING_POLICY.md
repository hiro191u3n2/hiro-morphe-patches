# microG Maps strict shop-name matching policy

This is a pure Java 8 component for the authorized microG Maps patch. It does not
hook Maps, change a query, fetch results, remove pins, or publish an artifact by
itself. The Maps integration is responsible for supplying the committed search
query and each result's **primary place name**, and for applying the same decision
to both the list and the map-result data.

## API

```java
StrictNameMatcher.Query plan = StrictNameMatcher.classify(committedQuery);
if (plan.enabled) {
    boolean keep = StrictNameMatcher.matches(plan, primaryPlaceName);
}
```

- `Query.enabled`, `Query.reason`, and `Query.normalizedQuery` are public, final
  fields. The plan is immutable and may be reused across a result batch and later
  batches for the same committed query.
- `matches(plan, primaryPlaceName)` returns **true to retain** a result.
- A null/disabled plan or null/empty/unreadable primary name returns true. Missing
  data is not proof that a result is unrelated.
- `shouldKeep(query, primaryPlaceName)` is a convenience wrapper. Prefer one
  `classify()` call per query instead of reclassifying for every result.
- `normalize(String)` exposes NFKC normalization, case folding with `Locale.ROOT`,
  hiragana-to-katakana folding, format-character removal, and space normalization.
- No category, address, review, or secondary description should be appended to
  `primaryPlaceName`; doing so would make unrelated results match a mentioned word.

## Behavior

For `ランダ`, both `ランダ銀座インズ店` and `RANDA 有楽町マルイ店` remain. `ダイアナ`,
`GINZA WASHINGTON`, `シューファンタジー`, and `リランドチュール` are rejected.

Known name aliases are explicit, local groups. RANDA/ランダ is included, along with
common brand aliases. There is no online dictionary, automatic transliteration,
fuzzy spelling correction, or inference that similar names represent the same
business. Unsupported English/Japanese alternate spellings can require a future
explicit alias. Unregistered names still use the generic normalized matching rule.

Spaces and punctuation in brand typography are ignored when locating a name.
Their original positions are retained for word-boundary checks. For example,
`RANDA` does not match `MIRANDA` or `RANDALL`, and `ランダ` does not match `オランダ`,
`グランダ`, or `ランダムカフェ`. Branch names following spaces are permitted.
Concatenated branch suffixes use Japanese script boundaries, common locations
and shopping-center names, and a bounded kanji branch-suffix rule.

Known aliases can be found in `銀座 ランダ`, `ランダ 銀座`, `銀座のランダ`, and
`ランダ銀座インズ店`. Location restrictions continue to be Maps' responsibility;
the matcher only enforces the business name. For unknown names, separated known
location qualifiers are stripped, e.g. `銀座 しゃぶ耀`. Arbitrary concatenated
prefixes are not stripped, because they may be part of the real business name.

Generic categories remain native searches, including `靴屋`, `喫煙所`, `カフェ`,
`銀座の靴屋`, `喫煙できるカフェ`, and `shoe stores near me`. Category detection uses
explicit category vocabulary, ordinary location/qualifier words, and conservative
handling of category words separated from unknown qualifiers. Address-like
queries, routes, coordinates, plus codes, geographic searches, very short or long
ambiguous inputs, natural-language requests, and known names with ambiguous
additional terms also bypass name filtering.

## Limits the integration must preserve

- This is a conservative text classifier, not a universal brand/category
  recognizer. Unregistered categories and unusual spellings cannot always be
  distinguished from a business name. If Maps exposes a trustworthy categorical
  search flag, the integration should use it to bypass filtering.
- A missing name is deliberately retained rather than treated as an unrelated
  place. The integration should report or count this case if useful for diagnosis.
- Unknown phonetic/translated aliases are not automatically guessed.
- Unknown region names attached directly to a shop name, and uncommon all-kana
  branch suffixes with no separator, may require an explicit alias or vocabulary
  update. Generic unknown names with ordinary spaces still work.
- This only filters results returned by Maps. It cannot restore a matching shop
  omitted by Maps' server or promise a complete inventory of branches.
- Tests here validate the matching policy, not Android pin rendering, Maps
  lifecycle, protobuf integration, or the safety of the final APK patch.

## Host validation

Source:
`src/app/hiro/mapsstrict/StrictNameMatcher.java`

Test entry:
`test/app/hiro/mapsstrict/StrictNameMatcherTest.java`
(`app.hiro.mapsstrict.StrictNameMatcherTest`)

Compile with Java 8 compatibility and run:

```sh
mkdir -p build
/usr/bin/java com.sun.tools.javac.Main --release 8 -encoding UTF-8 -d build src/app/hiro/mapsstrict/StrictNameMatcher.java test/app/hiro/mapsstrict/StrictNameMatcherTest.java
/usr/bin/java -cp build app.hiro.mapsstrict.StrictNameMatcherTest
```

Observed result: **231 behavioral assertions passed**. The tests cover the
provided RANDA screenshot, later result batches, Unicode spellings, registered
aliases, branch names, name-boundary false positives, general categories,
addresses/navigation, unknown business names, missing metadata, and independent
plans for consecutive searches. The code uses no Android or third-party classes.
