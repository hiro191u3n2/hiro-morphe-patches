# Bicycle glyph and CompactLegend filtering contract (v1.0.1)

## User failure evidence

The user reported that the gray circular bicycle POI marks remained after v1.0.0.
The previous release intercepted only `Lbkbv.b(Lcift;...)Lbkba;`, a SharedLabeler
placed-label factory. An APK transformation passing its structural tests did not
establish that the user's active rendering backend called that factory.

This revision adds a resolved legacy-icon predicate and a selective raw legend
transform. Native ingress and cache handling are documented separately. No
Android UI execution or on-device disappearance is asserted by this record.

## Verified glyph identity and URL grammar

The existing official Roadmap legend and fetched PNG evidence establish these
exact bicycle paths (the `.png` suffix is added by the legend):

- `poi/quantum/modern_pinlet/bike_pinlet_v3-2-medium`
- `poi/quantum/pinlet_88/bike_pinlet_v3-2-medium`
- `poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium`
- `road/icons/bike_pinlet_v3-2-medium`
- `transit/quantum/bikeshare-2-medium`
- `transit/quantum/bikeshare_v2-0-tiny`
- `transit/quantum/bikeshare_v2-1-small`
- `transit/quantum/bikeshare_v2-2-medium`

`Lbklf.a(String,Z,I,Lchyy;)String` constructs a relative URL from global prefix,
indexed path, local path, and global suffix. The real saved legend's root field
11 is `icon/name=assets/icons/`; root field 12 is `.png&scale=4`. `Lbken.j` also
accepts a bare asset path from feature overrides and adds `icon/name=` and
`&scale=4` at code offsets 0x93–0x9a. The predicate now accepts an exact bare
`assets/icons/...png` path as well as the previously validated icon URL forms.
It does not classify place names, search text, directions artwork, arbitrary
URLs, or an icon merely containing the word `bike`.

The drawable resource `bike_pin` is a plain balloon background, not the target
cycling-person glyph. Shared circle/background assets are never globally blanked.

## Legacy icon factory predicate

Hook signature:

```text
Lbkex;->b(Lbken;Lcom/google/common/collect/ImmutableList;Lchvn;Lbkjj;Lbkeo;Lbkrf;Z)Lbkex;
```

It is static, has 18 registers, and receives the override list in v12 and the
resolved style in v14. The new runtime signature is:

```text
Lapp/hiro/mapsbikes/BicycleMarkerFilter;->shouldHideLegacy(Ljava/lang/Object;Ljava/lang/Object;)Z
```

The first argument is `bkjj` (style), and the second is the native Guava
`ImmutableList` of per-feature overrides. The runtime verifies these exact
public field/method shapes and leaves unknown structures to the original code:

| Owner | Member | Meaning established by original consumers |
| --- | --- | --- |
| `bkjj` | `t:bkje` | Resolved icon style |
| `bkje` | `b:String` | Single-image URL |
| `bkje` | `c:ImmutableList` | Composite icon components |
| `bkjc` | `b:String` | Component image URL |
| `bken` | static `j(ImmutableList,bkje):bkje` | Native feature override resolution |

For nonempty overrides the runtime calls the actual native Java resolver,
instead of reproducing image/bitmap override precedence. This prevents a default
bicycle template that has become a store/custom image from being classified
using its obsolete default. The source style and lists are never mutated.

Returning null from this factory is already supported. `Lbkaw.p` checks the
factory result at 0xcc and returns null at 0xf4; `Lbkcz.k` then follows its
no-label path at 0x2f5. The second direct call in `Lbkcz.k` checks null at 0x789
and follows its existing no-object branch. The independent APK audit confirmed
both call sites. Existing labels cached in a running process are not retroactively
invalidated by this factory hook; installation/restart starts a new process.

## CompactLegend wire schema

`Lchyy.c(III)V` indexes root fields 3, 9 and 10 into `chyy.e`, `chyy.j` and
`chyy.l` respectively. The original consumer `Lbkjn.p(J)Lbkkn;` parses root 3
as `Lchvx` and passes its icon references to `Lbkje.c`.

| Location | Wire field | APK field / use |
| --- | --- | --- |
| Root | 3, repeated message | LabelStyle (`Lchvx`) |
| Root | 9, repeated message | IconStyle (`Lchuz`) |
| Root | 10, repeated string | Indexed icon path table |
| Root | 11 / 12, string | Global image prefix / suffix |
| LabelStyle | 3, repeated message | Inline icons (`chvx.f`) |
| LabelStyle | 5, message | TextBoxStyle (`chvx.i`, `Lchyj`) |
| LabelStyle | 7, bool | Hidden (`chvx.m`) |
| LabelStyle | 8, repeated packed/unpacked integer | Root-9 icon indices (`chvx.g`) |
| TextBoxStyle | 6, repeated message | Inline icons (`chyj.r`) |
| TextBoxStyle | 9, repeated packed/unpacked integer | Root-9 icon indices (`chyj.s`) |
| IconStyle | 1, string | Local/direct image URL (`chuz.c`) |
| IconStyle | 5, integer | Root-10 path index (`chuz.e`) |
| IconStyle | 8, uint32 | Feature image override ID (`chuz.d` → `bkjc.d`) |
| IconStyle | 13, uint32 | Color override ID (`chuz.o`), not an image replacement |

The generated `Lchvx.dynamicMethod` info string gives field 7 type 4103
(bool with presence), followed by presence-bit index 11; the object table binds
that field to `m`. Field 8 has type 39 (packed int32 list) and binds to `g`.
`Lbkjn.p` at 0x337 reads `chvx.m`. A true value takes 0x3fa–0x404, which clears
text (`bkji.n`), text-box/background (`bkji.o`) and icon (`bkji.p`) styles.
The resulting `bkjj.f` hidden flag is checked by the original label builder.

All 15 matching icon descriptors in the actual fixture have no image override
field 8. Their image identity is fixed. The transform excludes any descriptor
with nonzero image override ID so a per-feature substitution cannot cause an
unrelated marker to be hidden globally. Color-only field 13 stays eligible.

For each matching complete LabelStyle, the transform appends `38 01` (field 7,
true), updating only that record's length. Existing already-hidden records stay
byte-identical. Root fields, record order, all untargeted style bodies, icon
descriptors, path strings and unknown fields remain byte-identical. It does not
delete a shared style/index or blank a shared image. Later complete styles that
replace an inherited icon also replace the hide flag in the original style
resolver (`bkji.c = chvx.m` on each such style).

Inputs are bounded at 16 MiB, 300,000 root fields, 65,536 entries per table, and
64 icon components. Malformed data, unknown signatures, image payloads and
unsupported formats return the original array. No input array is mutated.
The function handles raw protobuf bytes. The saved source is raw protobuf;
HTTP compression behavior must be established by the ingress audit, and gzip
payloads are not claimed to be supported by this raw transformer.

## Actual fixture verification

Source URL:
`https://www.gstatic.com/maps/res/CompactLegend-Roadmap-b7c5817f3a154e40c111c4696d749e1d`

- Input SHA-256: `c63aaba03ff7c812c6644ad489f75812c62465e87c63a36d09cccd7d839dfe2d`
- Input bytes: 2,115,516; output bytes: 2,115,606.
- Output SHA-256: `cfeeaa10c623a16b9df085db1358f11f186c3edcb9ed6540c2066e1a116474ab`.
- 92,576 root records; 13,700 LabelStyles; 8,799 IconStyles; 943 path strings.
- 15 fixed bicycle descriptors; 60 referencing LabelStyles.
- 45 newly hidden styles; 15 already hidden styles kept byte-identical.
- All 92,531 remaining root records byte-identical.
- Input unchanged; second transform returns the same array reference.

The expected changed style-index set was derived independently with a Python
protobuf inspection of the real downloaded fixture. The Java golden-file test
uses that fixed set and an independent wire reader to compare every root record;
it does not use production predicates to choose what changes are permitted.
Indices are used only by that pinned-fixture test. Production always identifies
the exact icon path through the current table.

`BicycleMarkerFilterTest` passed 352 fixture assertions. The separate
`BicycleLegendFilterTest <legendfile>` passed 246 fixture assertions plus the
complete real-file diff. These are host tests, not an Android rendering test.
