# Discord send-button semantic test

`semantic-check.py` checks the exact two Hermes functions affected by the send-button rendering patch. It decodes the original bytecode and the in-memory replacements described by `patch-spec.json`, then executes those instructions in a limited host interpreter with mocked React / React Native imports.

This is not a Hermes VM, Android emulator, or physical-device rendering test. It does not reproduce the disappearing-button bug on a screen or prove its root cause.

## External inspection dependency

The test imports `Bundle` and `OPINFO` from the Venus-Patches inspection tools. Obtain them from the exact source commit used during analysis:

- Repository: https://github.com/VenusIsJaded/Venus-Patches
- Version: v1.21.1
- Commit: `987b7a22d96d41a0ba80c060b485291e1c7bcc4b`
- Required directory: `scripts/hbc` (`hbc.py` and `ops.txt`)
- Source license: GNU General Public License v3.0; see `NOTICE.Venus`.

The dependency is not copied into this source package. Python 3.10 or newer is sufficient; no third-party Python packages are required.

```sh
git clone https://github.com/VenusIsJaded/Venus-Patches.git venus-inspection
git -C venus-inspection checkout --detach 987b7a22d96d41a0ba80c060b485291e1c7bcc4b
```

## Input and execution

Extract `assets/index.android.bundle` from the user's Venus-v351.11 / v1.21.1 APK. The test refuses any bundle other than SHA-256:

`1d9e14c841737e987a44a876bec064ce1a029ae142835bdebd6a7e706c69f47b`

The APK and its bundle are deliberately excluded from this source package. The test does not modify them. Its bytecode-analysis cache is created in a temporary directory and removed on successful completion.

```sh
python3 tests/semantic-check.py \
  --bundle /path/to/index.android.bundle \
  --hbc-dir /path/to/venus-inspection/scripts/hbc \
  --output /path/to/semantic-result.json
```

Omit `--output` to print the result without saving a result file. Run the command from the `discord-send100` directory or supply the test's full path.

## Assertions

The test compares 256 cases: both React Compiler variants, all 64 combinations of six state flags, and both Reduced Motion settings after patching. The state flags are voice-message eligibility, text presence, attachment presence, pending edit, required text content, and cooldown.

For every case, the patched output must equal Discord's original static rendering path with Reduced Motion enabled. Assertions also check:

- The original send callback is retained, and rendering never sends a message.
- Disabled state, cooldown, voice-message selection, and channel ID are retained.
- The native input's imperative text-state setter is retained.
- The explicit button container dimensions remain positive and unchanged.
- The patched functions skip `TransitionGroup` and `FloatingSlot`.
- The unmodified functions use those animation wrappers when Reduced Motion is disabled.

These are isolated render-state checks with fixed positive theme-token values. They do not emulate React scheduling, an actual animation race, keyboard changes, or repeated UI navigation.
