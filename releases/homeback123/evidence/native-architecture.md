# Native Back evidence for the supplied v447 APK

Input SHA-256: `153f84f6700fc5ebe29f918b4d59540dbd299e896fc6d04e4ca9b5d6014e7dd2`.
Its 74 OneBack runtime classes match the distributed v1.1.12 classes. The prior
update is present; an install/update failure is not the explanation established
by this APK.

## Two paths requiring interception

`X.2xZ.A0V()` obtains the current Fragment via `X.2yE.A05()`. A current `Peq`
handler returning true branches to the common handled epilogue. If unconsumed
and the native tab stack has more than one entry, original PC315 pops that stack
and later calls `A0B` to change tabs. The new four-instruction gate immediately
precedes the pop, using the original Fragment in v1, navigation in v4 and dead
scratch v0. On success it branches to original PC444, retaining trace cleanup
and the handled return. Failure continues to the original pop. The whole body
and try-handler mapping are checked after removing the injected gate.

This alone is insufficient: `X.8Zv.onBackPressed()` can call Main `GfT`, which
calls `Dho.A00` with the configured default tab or FEED. `Dho.A00` calls
`X.2xZ.A0R` and subsequently trims the stack; it does not require `A0V`. The
already-installed Reels hook at original PC31 precedes this path. Its runtime
now trusts that exact native boundary instead of reclassifying the view tree.

## Consumed layers return before the Reels hook

The `8Zv` method first checks native `A1Y` popup state, dismisses via `A09` and
returns true. It next checks `A0L` / `A15`, calls `X.0wS.A0p` and returns true
when handled. `A0p` returns true both when its current child consumes Back and
when it closes the sheet itself. These returns precede the PC31 hook.

`X.22c` delegates Back to its selected child. `X.0jG` / `X.3gY` via
`X.3gU.A04` return immediately when the selected child consumes Back, before
their own FragmentManager pop. `X.9ld` likewise returns when its selected child
consumes Back. No blanket permission to exit is inferred from a modal existing.

## Limits

The static routes and the insufficient old runtime conditions are established.
Without a device trace, the exact rejected predicate and route used in the
reported session are not established. Physical Android execution is not tested.
Host guard models complement, and do not replace, emitted DEX branch validation.
