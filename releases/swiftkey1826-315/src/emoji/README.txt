SwiftKey 1.8.26 emoji related-term expansion

The existing 14,482 normalized dictionary readings and all 50,816 ordered emoji associations remain intact. Curated groups add 2,424 readings and 6,203 associations, yielding 16,906 readings and 57,019 pairs. Each original key's associations remain an exact ordered prefix; new associations append after them. All emoji sequences already exist in the baseline inventory. Existing Unicode attribution is unchanged.

Coverage includes conversational greetings, gratitude/apologies, emotions and reactions, food/drink, pets/animals, weather/seasons, work/study, games/media, sports/travel, daily chores, health/recovery wishes, celebrations, devices and common status messages. Hiragana and normal written forms are explicitly represented. Existing runtime normalization covers katakana and halfwidth katakana; Japanese-mode romaji reaches them via the native converter. No English-mode change, substring/suffix matching, inferred sentence truncation, or one-character broad matching is introduced.

Build data only:
python build_emoji.py --baseline BASELINE.tsv --output OUT

Validate against current exact runtime and pinned build tools:
python build_emoji.py --baseline BASELINE.tsv --output OUT --tools TOOLS --runtime RUNTIME.mpe

The production EmojiCandidates source recompiles to both exact published canonical DEX classes (EmojiCandidates and EmojiCandidates$1). The host executes the unchanged asynchronous production asset loader and every dictionary exact row except the pre-existing '#' row, which the existing loader treats as a comment. It exercises every unique curated input spelling, candidate identities/order, full raw composition/request commit metadata, duplicate suppression, separators, native converter boundary, kana/katakana/halfwidth handling, exact and prefix behavior, zero-input and non-Japanese modes, null/short input fallback, and a 4,000-candidate exact result.

The Java native converter is a test double: it verifies input forwarding and downstream lookup, not on-device native conversion execution. Handset UI, rendering and native interaction remain untested.

Changing swiftkey/emoji_ja.tsv requires replacing its embedded SHA-256 digest in the JVM and Android JapaneseSamsungEmojiPatch loader. The surrounding publication build performs and verifies those updates independently. No emoji runtime logic is changed by this component.
