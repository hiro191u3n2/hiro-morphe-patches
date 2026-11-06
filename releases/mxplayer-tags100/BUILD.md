# Build MX Player tag-row patch

Use JDK 21.0.13 and the exact tool hashes in `src/build.py`.

```sh
python3 src/build.py --base BASELINE357.mpp --tools TOOLS --java-home JDK21 \
  --work FRESH_BUILD --dist EMPTY_DIST --apk MX_BASE.apk --bytecode-mode FULL
```

The same patch was independently rebuilt and applied in STRIP_FAST mode; both MPP archives matched byte for byte. Supplied APKS contained base.apk, split_config.arm64_v8a.apk and split_config.xxhdpi.apk. Only base.apk's row insertion hook is changed. The two configuration splits remain unchanged.

The original app/APKS is not included in this source release. Android UI execution was not tested.
