# ULike1.9.36 publication

The workflow rebuilds the front preview and lens-visibility fix from immutable ULike1.9.35 / Hiro Morphe1.0.168 inputs using the pinned toolchain from successful1935 CI run37603666483. It preserves the1935 ARM64 kernel and installer bytes.

Before publication, manifest.json must be filled with the six exact output artifacts, complete DEX/archive deltas, source inventory, successful host results and both original-APKS applications. The publisher verifies these bytes and the active main/dev feeds, publishes the release, advances both feeds atomically and verifies public assets and the receipt. Existing image-quality, speed/save code and other apps are retained.

No Android/Galaxy hardware execution is claimed.
