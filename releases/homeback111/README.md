# Trip.com home-only Back 1.1.1

Trip.com (`ctrip.english`) now uses the same home-only exit policy as X, TikTok and Instagram. Other applications, including a different app named torip, are not added.

The Trip.com home recognizer requires the visible app-owned `com.ctrip.ibu.home.home.presentation.page.fragment.widget.HomeAppBarLayout` and a positively selected, interactive bottom home tab. This class name is present in the existing Trip.com home-resource patch. This is a structural contract, not an assertion that an original Trip.com APK or physical device has been tested.

All existing keyboard, dialog, fragment-backstack, same-key and native callback guards are used. Ambiguous UI states use native Back. Existing SNS recognition is unchanged. Only Trip.com is marked as needing this incremental update in the Manager changelog.

Host tests were run locally against production runtime source and Android doubles: 60 cases, 238 assertions. CI compiles against the pinned Android SDK, tests DEX preservation and ten fixture APK applications, repeats the complete build, and requires byte-identical MPPs and QA before publication. The publisher preserves unrelated bundle assets and ULike active policy, and updates main/dev feeds atomically. Neither an original third-party APK nor an Android device is available for this change.

Repatch Trip.com from an unmodified APK. Task removal is not Android Force stop and does not clear account/search history.
