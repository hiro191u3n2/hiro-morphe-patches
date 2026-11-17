# View count integer formatter

Runtime entry point: `Lapp/hiro/newxui/ViewCountFormat;->formatOrNull(Ljava/lang/Long;)Ljava/lang/String;`.

The scoped native bridge calls this only for the ViewCount action, before producing a compact native label. With `UiOptions.fullViewCounts()` ON, a nonnegative raw `Long` becomes a locale-aware full integer, such as `12,345` in Japanese/US English. Null, negative values, or setting OFF return null so the bridge can execute the original native formatter. The primitive-long overload preserves every 64-bit integer; no floating-point conversion is used.

A `ThreadLocal<FormatterState>` reuses one NumberFormat for each thread's current default Locale. A locale change replaces that state. Different threads never share mutable NumberFormat objects, and there is no growing count cache or locale map. OFF/invalid calls return before inspecting or creating the cache. The runtime includes the additional private class `ViewCountFormat$FormatterState`.

Run `python3 test.py` to compile the actual parent UiOptions and formatter with the host-only Registry fake. The test verifies exact boundary values, null/negative/OFF fallback, toggling back ON, five locales, locale replacement and equal-locale reuse by private reflection, 1,000 reuse calls, and 4,000 concurrent calls across four threads. Thread identity checks verify that each worker owns a distinct NumberFormat. The fake Registry, tests, benchmark, and host build output must not be packaged in Android DEX.

The same command runs a host JVM microbenchmark against v380's per-call NumberFormat construction. It uses preboxed count inputs, consumes the returned strings, warms up both variants, alternates their measurement order, and records per-thread allocated bytes where the JVM supports that measurement. All individual samples are recorded in `benchmark.json`.

The recorded OpenJDK 21 run (50,000 calls per round, five rounds, en-US) measured median allocation of 59,598,480 bytes for the old runtime formatter versus 5,998,480 bytes for the reused formatter, about 90% less. Median elapsed time was 73,098,193 ns versus 32,795,364 ns. These are host formatter measurements, not Android scrolling/frame-time measurements. They exclude the removed native compact-formatting work in the bridge. Actual device performance was not measured, and timings may vary on rerun.
