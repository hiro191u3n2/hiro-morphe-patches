import app.morphe.manager.util.*
import java.io.File

data class Change(val version: String, val kind: String, val pkg: String, val patch: String)

fun main(args: Array<String>) {
    val (catalogPath, markdown, changesPath, baseline, current) = args
    val catalog = File(catalogPath).readLines().map { it.split('\t') }
    val history = File(changesPath).readLines().map {
        val (version, kind, pkg, patch) = it.split('\t')
        Change(version, kind, pkg, patch)
    }
    val text = File(markdown).readText()
    val stable = ChangelogParser.parse(text)
    check(stable.first().version.normalizeVersion() == current.normalizeVersion())
    val versions = listOf(baseline) + stable.map { it.version }.filter { isNewerVersion(baseline, it) }
    val allNames = catalog.map { it[1] }.filter(String::isNotBlank).toSet()
    val byPackage = catalog.groupBy { it[0] }
    var checks = 0
    for (dev in listOf(false, true)) {
        val channel = if (dev) ChangelogParser.parse(text, stopAfterFirstStable = true) else stable
        for (installed in versions) {
            val entries = ChangelogParser.entriesSince(channel, installed, if (dev) stable else emptyList())
            val changes = history.filter { isNewerVersion(installed, it.version) }
            for ((pkg, rows) in byPackage) {
                val packageNames = rows.map { it[1] }.filter(String::isNotBlank).toSet()
                val names = packageNames + "ローカライズ名"
                val patches = rows.map { it[2] }.toSet()
                val selections = patches.map { setOf(it) } + listOf(patches, emptySet())
                for (selection in selections) {
                    val expected = changes.any {
                        (it.pkg == pkg || it.pkg == "*") && (it.kind == "added" || it.patch in selection)
                    }
                    val subject = ChangelogSubject(names, selection, pkg, allNames - packageNames)
                    val shown = isNewerVersion(installed, current) &&
                        ChangelogParser.hasChangesFor(entries, installed, subject)
                    check(shown == expected) {
                        "$installed -> $current (dev=$dev), $pkg / $selection: expected $expected, got $shown"
                    }
                    checks++
                }
            }
        }
    }
    println("Verified ${byPackage.size} packages, ${versions.size} installed versions, $checks badge checks")
}
