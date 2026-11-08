# Maps bicycle icon predicate evidence

## Result

Eight exact bicycle/bike-share icon paths are present in the current official normal Roadmap style. All eight corresponding Google `vt/icon` requests returned HTTP 200 and valid PNG files. This supplies actual asset names for a narrow map-label filter.

The cyclist icon basename is **`bike_pinlet_v3`**. It is distinct from generic route-mode bicycle icons and from the AR category-color key `bicycle_rental_service`.

## Official source and extraction

The anonymous public [Akihabara map viewport](https://www.google.com/maps/@35.6993,139.7764,19z?hl=ja) supplied base64 protobuf configuration in `window.APP_OPTIONS[3][6][13]`. Its `StyleTableMapping` records identify this normal-map style resource:

[CompactLegend-Roadmap-b7c5817f3a154e40c111c4696d749e1d](https://www.gstatic.com/maps/res/CompactLegend-Roadmap-b7c5817f3a154e40c111c4696d749e1d)

- Verified at UTC: 2026-11-08T15:24:08.638267+00:00
- HTTP status: 200.
- Bytes: 2,115,516.
- SHA-256: `c63aaba03ff7c812c6644ad489f75812c62465e87c63a36d09cccd7d839dfe2d`.

The table below uses zero-based protobuf indices. `10[n]` is a complete path in the root string table. The `bikeshare_v2` URL templates use field 5 value 37, which resolves to root string-table entry `transit/quantum/`. These indices identify this source snapshot; they are not category IDs used by the runtime.

## Verified assets

Each request has the exact form `https://www.google.com/maps/vt/icon/name=assets/icons/<path>&scale=4`. Full URLs and SHA-256 values are in the accompanying JSON.

| Asset path after `assets/icons/` | Legend reference | HTTP | PNG pixels | Bytes | SHA-256 prefix |
|---|---|---:|---:|---:|---|
| `poi/quantum/modern_pinlet/bike_pinlet_v3-2-medium.png` | 10[272] | 200 | 112 × 128 | 693 | `1b2927d30532ac81…` |
| `road/icons/bike_pinlet_v3-2-medium.png` | 10[392] | 200 | 80 × 80 | 523 | `8831803c56092738…` |
| `poi/quantum/pinlet_88/bike_pinlet_v3-2-medium.png` | 10[667] | 200 | 88 × 88 | 854 | `8bd1db1c346847bd…` |
| `poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium.png` | 10[766] | 200 | 104 × 104 | 1525 | `6bff57e769952557…` |
| `transit/quantum/bikeshare-2-medium.png` | 10[928] | 200 | 84 × 84 | 2667 | `70722975da4f92d2…` |
| `transit/quantum/bikeshare_v2-0-tiny.png` | 9[1881] → 10[37] | 200 | 48 × 48 | 1828 | `96032557b7da4195…` |
| `transit/quantum/bikeshare_v2-1-small.png` | 9[7726] → 10[37] | 200 | 60 × 60 | 2019 | `b08311df69cc0cfa…` |
| `transit/quantum/bikeshare_v2-2-medium.png` | 9[7727] → 10[37] | 200 | 84 × 84 | 2452 | `bd4ce6e040eabf71…` |

## Visual correspondence and scope

All four `bike_pinlet_v3` namespace variants were visually inspected. They depict a person riding a bicycle, rendered as a blue tint-mask source. The supplied screenshot contains the same cyclist/bicycle glyph family in white inside small dark gray markers. This is a visual correspondence inference; no icon URL was captured from the user's device.

The four `transit/quantum/bikeshare` variants were also inspected. Their pink source masks depict a bike-share dock/vehicle form and differ from the cyclist pinlet. They are separately supported by the same official normal Roadmap style.

The proposed filter uses exact paths in placed point-label icon components. Generic `directions_bike`, `activity_cycling`, and `protected_bike_lane` assets are outside the eight-path predicate. No local bicycle drawable needs to be blanked, and this icon investigation requires no native-library change.

## Verification boundary

This record verifies the official normal-map style references, successful asset downloads, image hashes and dimensions, and visual inspection. No application was installed or launched for this record, and disappearance of the markers on a device has not been observed. APK patch-application and structural tests are separate verification records.

The PNGs and full third-party legend are not included in the source package; this record preserves their official source URLs and hashes.
