# View count integer formatter

Runtime entry point: `Lapp/hiro/newxui/ViewCountFormat;->format(Ljava/lang/Long;Ljava/lang/String;)Ljava/lang/String;`.

The scoped patch supplies the **raw nullable Long view count** and its native rendered text. When `UiOptions.fullViewCounts()` is ON, a nonnegative count becomes a locale-aware full integer (for example `12,345` in Japanese/US English). The code invokes NumberFormat's primitive-long overload and never converts through floating point. Null/negative data and setting OFF return the original String reference unchanged, including a null original value.

NumberFormat is constructed per call to avoid sharing mutable format state between threads and to respect changes in the default locale. This class does not select counters or inspect model types: hook scoping must separately ensure that likes, replies, reposts, bookmarks, and other counters never call it.

Run `python3 test.py` to compile the actual parent UiOptions and formatter with the host-only recording Registry fake. Tests cover 64-bit values beyond double precision, Long.MAX_VALUE, locales, all fallback conditions, and concurrent formatting. Do not package the test class, fake Registry, or host build output in Android DEX.
