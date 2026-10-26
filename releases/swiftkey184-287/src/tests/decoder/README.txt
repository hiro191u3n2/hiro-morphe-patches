Decoder host regression tests (Java 8 source, no Android or external dependencies)

Compile:
java com.sun.tools.javac.Main --release 8 -encoding UTF-8 -g:none -d CLASSES src/hiro/swiftkey/JapaneseGlideDecoder.java tests/decoder/DecoderRegressionTest.java tests/decoder/DecoderAssetValidationTest.java

Run in separate JVMs:
java -Xmx256m -cp CLASSES DecoderAssetValidationTest GLIDE_BIN
java -Xmx256m -cp CLASSES DecoderRegressionTest GLIDE_BIN

GLIDE_BIN is the existing asset (SHA-256
8f21e641a8b7f86c4bc26252be07515f9a4f402e4a2a516187d6194e7b264a4e).

Embedded geometry fixtures reproduce the archived target SwiftKey Beta
9.13.16.4 layout resources and native fb0.b -> j70.a -> fc0.m1.a arithmetic:
standard ROMAJI, ROMAJI PC, standard with split_add=0.1/split_extra=true.
The archived modified v2 compact resource equals standard, so no separate
claim is made about unmodified compact geometry or an Android layout test.

Trace cases cover 56 words/romanizations on all three layouts with ideal
center paths, coordinate drift+jitter, and sparse sample intervals. They
include repeated-letter and collinear ambiguity, short forms, greetings,
and dictionary-only vocabulary not present in the supplemental list.
These tests assert intended readings remain selectable, not that intent
can be inferred from an ambiguous shape. They do not measure natural
input accuracy, Android event handling, native Japanese conversion,
candidate UI operation, or device latency.

Implementation limits:
- The existing 895,801 dictionary rows remain byte-identical. Supplemental
  short forms and greetings do not constitute a full new language model.
- At most 150,000 rows and 192 fine candidates are processed; output has
  at most 12 distinct readings. Search is deterministic and has no wall
  clock cutoff. A count-limited search retains its available candidates.
- Shape-equivalent words need explicit user choice. No learned context
  or persistent private input history is added.
- The novel-reading fallback is deliberately marked Result.fallback;
  it is only a geometric guess, must pass the native conversion check,
  and must never be committed without user selection. Some new words
  and arbitrary sentences cannot be recovered from their traces.
- Single-letter stationary decoder support does not turn a normal tap
  into a glide. Tap handling stays with the native keyboard.
