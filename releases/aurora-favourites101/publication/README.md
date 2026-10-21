# Aurora Store publication v1.0.1

Pinned legacy and Patcher 2 toolchains exercise both APK distributions. The workflow rebuilds the locally validated MPP twice, verifies standalone/combined/QA outputs and local artifact identities, excludes both APKs from public assets, publishes exact immutable downloads, and updates main/dev Manager feeds atomically.

The aggregate replaces only the existing Aurora loader/runtime entries and manifest; unrelated resources and the ULike active policy are preserved. Publication refuses a changed active baseline or concurrent repository update.

After final local build and validation, run `publication/freeze.py --dist DIST`, `publication/package.py --dist DIST`, and `publication/publish.py --dist DIST --base BASE --repo REPO --local-only`.
