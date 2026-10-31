# Instagram Back correction — OneBack 1.1.27 / bundle 1.0.318

Instagram 452.1.0.53.64 can select a native `FEED_SWITCHER` Home route that the
previous runtime classified as another page. This release follows its cached
stable page IDs through `0A4c` and `052y`, and recognizes Home only when the
selected content is the native Home fragment. It never initializes a hidden
tab while inspecting a modal profile.

The original Home receiver also has an exit-confirmation branch that can show
a second-Back prompt and refresh the feed. The new fallback runs only at that
verified branch, after the original child Back returned false. It preserves the
initial Activity, task, fragment and View, key/predictive origin, cancellation,
input windows, dialogs, and same-Back child-return protections. Existing native
instructions and control flow are independently restored and compared to the
original APK after removing only the validated hook instructions.

Native-created external profile links now permit one valid web `hl` language
parameter, including `hl=ja`. Unknown navigation parameters, internal links,
duplicate/invalid language values and unsupported URL forms remain rejected.
The native profile handler still decides whether Back closes a popup; profile
videos still return to their profile before a later Back can exit.

These are verified gaps in the supplied APK and previous runtime. They are not
proof of the exact server-selected route or URL query on the reporting handset.
Navigation Bar being disabled did not resolve the user's symptom. No physical
Android handset was available for this release: **実機未確認**.

## Validation

- Two independent clean builds produce identical MPPs and build reports.
- All 34 host suites pass, including 109 new cases / 615 assertions.
- The old 317 runtime fails seven of the new FeedSwitcher cases and fifteen of
  the locale-profile cases; existing rejection cases remain protected.
- The native Home DEX audit verifies both original executable bodies, incoming
  branches, argument registers, and rejection of altered native contracts.
- Application using Manager 2.3.0-dev.7's exact embedded host succeeds on the
  original APK, both with Hiro alone and with Piko 3.11.0-dev.3's 64 default
  patches. Both requested source-bundle orders pass independent full-APK
  preservation audits and produce identical APKs; Manager uses the same sorted
  patch execution order in both cases. The final evidence is under
  `verification/` and is checked before any distribution update.
- 282 unrelated loader definitions and 86 unchanged runtime definitions retain
  their previous bytes. Seven existing runtime definitions change and five
  native-Home helper definitions are added. Other applications' regression
  suites and all four supported loader metadata checks pass.

## Applying the update

Refresh the Hiro source in Morphe Manager, patch the clean matching Instagram
APK with 「戻る1回で終了・履歴削除」 selected, and install the resulting APK.
Updating a source does not itself rebuild the installed application. Existing
unknown modified APKs remain rejected by the established upgrade checks.

The public source contains no original Instagram APK, native APK dumps, or user
screen recording. Distribution preserves the current unrelated patch bundle
contents and atomically updates both Manager feeds after verification.
