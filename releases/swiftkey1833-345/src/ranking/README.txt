SwiftKey 1.8.33: preserve current prediction order

The only production change from this directory is the body of
Lhiro/swiftkey/FrequencyLearning;->rank(Ljava/util/List;Ll40/c;Z)Ljava/util/List;.
The class in src is a method donor, not a replacement runtime class. Its field,
native reading declaration, constructor, and every stub must be discarded.
The merge must preserve every other FrequencyLearning method, its original
fields/annotations, all nested classes, and all other runtime classes except
the separately reviewed SpaceConversion change.

Before this change, the additional usage-history ranker moved any historically
frequent spelling ahead of the input list. Its store does not retain sentence
context. The new method retains the cleaned input list in its existing order
and appends only historical spellings absent from that list. The existing
store still chooses the appended entries and their count/recent ordering.
Existing cross-reading frequency totals are retained; they can affect the
appended history order, but cannot move an existing candidate.

This preserves current native and supplementary candidate positions, identities
(except the unchanged gloss cleanup), and already-present duplicates/nulls.
It does not reorder dictionary candidates by frequency. The existing reading
normalizer, gloss cleanup, storage format, recorded counts, save worker,
forget behavior, session/privacy guards, and all dictionary data are retained.

Run:
  python ranking/run_rank_tests.py --tools TOOLS --runtime PUBLISHED_1832_MPE --work FRESH_WORK

Outputs: FRESH_WORK/fixed.dex and FRESH_WORK/report.json. The donor DEX contains
one class shell; the build must graft only rank, never the whole class.

Baseline proof: baseline/FrequencyLearning.java is reconstructed from published
1.8.32. Compiling with the pinned Java 8 target and D8 settings reproduces the
published rank method's canonical smali exactly, including instructions,
registers, branches, catches and annotations. The runner verifies this on every
build against both the input runtime and the pinned method fixture.
The unchanged UsageBridge.rank boundary is likewise reconstructed and compared
before its host tests exercise blocked and revoked session outcomes.

The six old ordering regressions use modeled native preference lists. They
demonstrate that history overrides such preferences in 1.8.32 and preserves
them in 1.8.33. They do not measure SwiftKey's actual choice for those Japanese
sentences, language-model accuracy, or Android behavior. Store ranking,
normalization, and cleanup are host models bound to the retained method
fixtures; storage persistence is protected by method/class preservation rather
than a substitute filesystem implementation. Privacy tests exercise the
unchanged session-authorization boundary; native private-field detection is
retained, not reimplemented by the tests.
