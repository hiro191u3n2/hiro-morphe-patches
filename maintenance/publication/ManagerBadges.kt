import app.morphe.manager.util.*
import java.io.File

fun main(args: Array<String>) {
    val (catalog, markdown, previous, current) = args
    val rows = File(catalog).readLines().map { it.split('\t') }
    val allNames = rows.map { it[1] }.filter(String::isNotBlank).toSet()
    val text = File(markdown).readText()
    val stable = ChangelogParser.parse(text)
    check(stable.first().version.normalizeVersion() == current.normalizeVersion())
    var checks = 0
    for (dev in listOf(false, true)) {
        val channel = if (dev) ChangelogParser.parse(text, stopAfterFirstStable = true) else stable
        fun shows(version: String, subject: ChangelogSubject): Boolean {
            if (!isNewerVersion(version, current)) return false
            val history = if (dev && ChangelogParser.stopsAbove(channel, version)) stable else emptyList()
            return ChangelogParser.hasChangesFor(ChangelogParser.entriesSince(channel, version, history), version, subject)
        }
        for ((pkg, label, patch) in rows) {
            // Exercise each selectable patch alone, even with a localized or cloned label.
            for (name in listOf(label.ifBlank { "名称未登録" }, "別の表示名")) {
                val subject = ChangelogSubject(setOf(name), setOf(patch), pkg, allNames - name)
                check(shows(previous, subject) && !shows(current, subject)) { "$pkg / $patch / $name" }
                checks++
            }
        }
        if (rows.any { it[0] == "com.twitter.android" }) {
            for (name in listOf("Twitter", "X", "NewX")) {
                val subject = ChangelogSubject(setOf(name), packageName = "com.twitter.android")
                check(shows(previous, subject) && !shows(current, subject)) { name }
                checks++
            }
        }
        check(!shows(previous, ChangelogSubject(setOf("UnrelatedApp"), packageName = "invalid.unrelated")))
    }
    println("Verified ${rows.size} package/patch pairs; $checks stable/dev selection and old/current version checks")
}
