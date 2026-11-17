# Full timeline view counts

`ViewCountHook` targets X `12.43.0-prod.01`, `com.x.inlineactionbar.w.c`. The method's exact native fingerprint is checked before mutation. It is identical in the original DEX and all four tested v379 configurations (Piko dev.1, dev.2, dev.2 clone, and standalone dev.2).

The inline action model carries an action enum, a nullable `Long` count, and an enabled flag. The native data mappers parse the server's decimal count string in radix 10 into `Long`; the count is not reconstructed from the abbreviated label. Null stays null. The hook neither invents extra precision nor alters any action's visibility.

Only the eight instructions that produce an inline action's nullable label are replaced. A private static synthetic bridge receives the still-live model and native `Resources`, performs the same null check and original native formatting, and passes the raw boxed `Long` plus original label to `ViewCountFormat.format` only when the enum is `ViewCount`. Likes, reposts, replies, bookmarks and every other action return the native result. The separate money-like count formatting loop is unchanged.

The original result register remains v7. The other original scratch register v8 is unconditionally overwritten by `Iterator.hasNext()` before either the next element or loop exit. The native method retains its 61 registers, every unrelated instruction, all external control-flow targets, Compose groups, and layout logic. The bridge has six registers and explicitly overwrites both halves of its long temporary before using them as references. The new method accesses existing native fields and formatter from the same class as the original caller.

Piko remembers formatted labels in Compose state, so the setting is marked as requiring an X restart. Formatting changes no dimensions or overflow behavior; exceptionally long counts remain subject to the native layout. Physical-device layout testing has not been performed.

Run `python views/test.py ORIGINAL_CLASSES6.dex [BASELINE.apk ...]` from a source tree containing the documented build tools. The host audit exercises actual Patcher 2.4 mutable DEX APIs, verifies every preserved native instruction and relocated branch target, serializes and reloads the changed DEX, rejects duplicate application, and interprets the emitted bridge for 126 combinations of action type, nullable/zero/negative/64-bit counts, and enabled state. Runtime locale formatting is tested separately in `views-runtime`.

This directory contains no redistributed application DEX or source dumps.
