# Initial external-profile Back — OneBack 1.1.28 / bundle 1.0.319

Instagram 452.1.0.53.64 can attach a native-created external profile directly to
MainActivity's root container when its selected tab wrapper is unavailable.
The previous runtime asked the Home/tab classifier to identify that profile.
The native tab selector can return null or a retained tab wrapper even while
the registered profile is the root container's current fragment. This left
that entry to ordinary native navigation, which can reveal Home on Back.

The correction recognizes the exact registered profile through its Activity,
parent FragmentManager and root container. It retains the native Activity layer,
window, input, popup, profile-tab and pending-transaction checks. The profile's
original Back handler still runs first; its consumed result remains authoritative.
The existing key, predictive gesture and same-dispatch child-return guards remain
in force, so returning from a profile video does not become an immediate exit.

The initial-entry tests keep Main's native selector null or pointed at a retained
tab, matching the relevant native API limitation. Earlier positive fixtures made
that selector return the profile itself and therefore missed this case.

The supplied recording of bundle318 shows the first profile Back revealing Home
and the next opening returning directly to the browser. The recording does not
identify the internal Activity/Fragment route. This release fixes the verified
root-entry gap; its exact relationship to the handset's path still requires
device confirmation. **実機未確認** for bundle319.

## Validation and scope

Two independent clean builds produced identical MPPs and build reports. All 35
host suites passed, including 38 new initial-profile cases and 276 assertions.
The same initial-profile tests against 318 failed 17 cases while 21 retained cases
passed, demonstrating that the new tests exercise the missing first-entry route.

Using Manager 2.3.0-dev.7's exact embedded host, the final MPP applied successfully
to the original APK alone and with the official Piko 3.11.0-dev.3 default 64
selection in both requested source-bundle orders. Actual patch event order was
recorded. The two mixed APK byte sequences differ; each separately passed all
native-body, runtime, manifest, resource and unrelated-ZIP preservation audits.
This verifies the tested source configurations, not every optional combination.

All three outputs retain the exact final 98 runtime definitions and the 12 native
hooks. Each initial-profile contract audit passed 59 assertions, including four
altered-body rejection cases. The native Home and profile-link audits also
passed. Evidence is under `verification/` and is checked before publication.
The exact runtime change is `InstagramExternalProfile`; the other 97 runtime
definitions and all 282 unrelated loader definitions retain their baseline 318
bytes. The additional loader contract checks the native initial-profile route
before any mutable patch operation.

## Applying the update

Refresh the Hiro source in Morphe Manager, patch the clean matching Instagram
APK with 「戻る1回で終了・履歴削除」 selected, and install the resulting APK.
Source refresh alone does not rebuild an installed application.

The release source includes authored patch code and verification tools, with no
original Instagram APK, native APK dumps or user recording. Both Manager feeds
are updated atomically after the distribution files are verified.
