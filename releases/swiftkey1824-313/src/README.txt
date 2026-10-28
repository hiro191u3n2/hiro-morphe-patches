SwiftKey 1.8.24 / combined bundle 1.0.313 — dictionary lookup repair

Problem
The bundled English and Japanese dictionary providers accept ASCII romaji before calling the native Japanese.romajiToHiragana converter. The QWERTY long-vowel key emits U+30FC directly. Native typing keeps the unconverted composition separately from its displayed hiragana, so raw strings such as airabuyuー reach request.k. The old providers reject this mixed input before lookup, even though the native keyboard converts it for display. Both providers now accept the direct long-vowel mark in this existing romaji path and pass the original string unchanged to the native converter. Other script guards and candidate commit metadata remain unchanged.

Japanese exact matches also stopped at 12 supplemental candidates. Only this exact-match cutoff is removed. The parser's existing 32-column ceiling and prefix search bounds (12 candidates / 64 rows / 32 ranks) remain. The current published dictionary has no more than 24 candidates for a reading. Full-data host tests compare every one of its 638,044 readings and 847,642 candidates, preserve order, and exercise existing-candidate deduplication.

Preservation
Input standalone: SwiftKeyBeta_v2_SamsungEmoji_v1.8.23.mpp.
Input combined bundle: Hiro_Morphe_Patches_v1.0.312.mpp.
Both carry runtime SHA256 dea2051af47cd8e2106437dbf7b3bc4f9ac2e6d496a334462700d7d2584e405a.
The merger changes precisely three method implementations in the existing 91 runtime classes:
- WordCandidates.isRomaji(String)
- KatakanaEnglishCandidates.isRomaji(String)
- WordCandidates.addWords(LinkedHashSet, String[], Set)
The compiled WordCandidates isRomaji body has no class-specific references and is reused for the identical English predicate. Existing method and class metadata are retained. No class, field, method, dictionary, or loader is added or removed. Replacing these three implementations with their baseline implementations reconstructs the original classes under canonical DEX comparison. Every non-manifest/non-runtime MPP entry is byte-identical, including all dictionaries, all JVM loader classes, the Android loader classes.dex, and other applications' resources.

Validation
The archived WordCandidates source is rebuilt with Java 17 --release 8 and D8 8.7.18 --debug and must exactly match all three published WordCandidates classes under canonical smali comparison. The fixed source differs in only the two named methods. Whole-dictionary tests reproduce the baseline 9,411 omitted candidates across 2,175 readings and confirm zero omissions after the change. Native conversion is a test double in JVM tests: tests assert that it receives the unchanged raw input and return known readings to test the actual downstream dictionary lookup and candidate construction. The native APK code separately establishes the conversion and raw-input storage contract.
The independent ABI audit uses the archived modified 9.13.16.4 APK, SHA256 e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7. This is not an untouched APK application test. The exact report is pinned to the final runtime and checker source; CI reuses this evidence without redistributing the APK. Real Morphe JVM loaders verify patch selection, dependency graphs, extension stream identity, and unchanged metadata. Two independent builds must reproduce identical publication artifacts.

Build
Use Java 17 for javac/D8 and set MORPHE_JAVA to Java 21 for the Morphe SDK.
python build1824.py --download --input-dir INPUT --tools TOOLS --work WORK --output DIST
The --compile-only mode produces the candidate runtime for independent native ABI review before the final evidence is embedded.

Limits
Host tests and static APK inspection do not prove Android handset rendering, native conversion execution, installed patch state, or touch behavior. Device-tested and original-APK-apply-tested remain false. The reports describe verified defects and code changes, not an assertion that the user's handset is already fixed.
