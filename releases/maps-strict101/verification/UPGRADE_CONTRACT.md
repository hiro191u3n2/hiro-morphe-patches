# Verified v1.0.0 to v1.0.1 upgrade

The published v1.0.0 runtime is embedded as
`contracts/maps_strict_search_v1.0.0.dex`, copied directly from immutable bundle
1.0.367 (SHA-256 `abd81b88ef43aeb7f8bed0fab9c39b0ab3a06eb8f1f3b5f634e7263ce60a41b3`).
It is a reference for the compatibility guard, not an extension installed into
the output APK.

## Acceptance

An APK without the strict-search runtime must satisfy all original request,
model, protobuf, and renderer contracts. An APK with that runtime must satisfy
the exact namespace inventory and canonical class bodies of published v1.0.0.
Unknown, partial, modified, and already-v1.0.1 installations are rejected.

For an upgrade, the guard copies the two native hooked classes into temporary
mutable objects. It verifies and removes exactly the five known response-hook
instructions, the one known merge-hook instruction, and the private transient
synthetic success marker from those copies. The recovered bodies must then
satisfy the original native contracts. This normalization never changes the
actual input APK's native classes.

## Replacement

The patcher extension merger does not replace existing method implementations.
The v1.0.1 provider therefore explicitly installs every extension class as a
`MutableClass` through the patch context's class collection. With STRIP_FAST,
this marks old runtime descriptors for replacement. All original runtime helper
class names remain present; new helper classes may be added. The two existing
native hooks remain bytecode-equivalent and are not inserted again.

## Release evidence

The build applies the new integrated MPP to both a v1.0.0-only APK and an APK with
v1.0.0 plus the three existing Maps options. It checks every resulting runtime
class against the new compiled extension, rejects leftover helpers, verifies
native hooks are unchanged, and preserves every other original class and
non-DEX resource. The same build exercises fresh standalone/integrated inputs,
both previously supplied APK variants, and all four Maps options together.
The exact source and artifact hashes are bound in the release QA JSON.

These are APK transformation and host checks. They do not establish Android
installation, Google server behavior, or live map rendering success.
