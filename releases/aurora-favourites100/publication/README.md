# Aurora Store publication

Pinned legacy and Patcher 2 toolchains exercise both APK distributions. The workflow rebuilds the locally validated additive MPP twice, verifies both outputs and local artifact identities, excludes the Aurora APK from all public assets, publishes exact immutable downloads, and updates main/dev Manager feeds atomically. Unrelated resources and the ULike active policy are preserved. Publication refuses a changed active baseline or concurrent repository update.

After completing local build and validation, run `publication/freeze.py --dist DIST`, then `publication/package.py --dist DIST` and `publication/publish.py --dist DIST --base BASE --repo REPO --local-only`. Both APK URLs and digests in input.json must match the build's verified official-signed and F-Droid inputs.
