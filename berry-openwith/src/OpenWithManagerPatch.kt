package app.hiro.berry.openwith

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.iface.instruction.Instruction

private const val EXT = "Lhiro/berry/openwith/OpenWithManager;"

private val target = Compatibility(
    packageName = "jp.ejimax.berrybrowser",
    name = "Berry Browser",
    description = "3.83.74 (383740) の元APKS専用",
    targets = listOf(AppTarget("3.83.74", 383740))
)

@Suppress("unused")
val berryOpenWithManagerPatch = bytecodePatch(
    name = "「他のブラウザで開く」の候補を長押しで管理",
    description = "「他のブラウザで開く」に表示されるアプリを長押しで個別に非表示にし、非表示アプリの復元もできます。通常タップの動作は変更しません。最後の1件は非表示にできないため、候補をすべて消して復旧不能になることを防ぎます。Berry Browser 3.83.74 (383740) 専用です。",
    default = true
) {
    compatibleWith(target)
    extendWith("extensions/berry_openwith_manager.mpe")

    execute {
        require(packageMetadata.packageName == "jp.ejimax.berrybrowser" &&
                packageMetadata.versionName == "3.83.74" &&
                packageMetadata.versionCode == "383740") {
            "Berry 3.83.74 (383740) の元APKSを選択してください"
        }
        require(classDefByOrNull("Lhiro/berry/openwith/OpenWithManager;") == null) {
            "既に「他のブラウザで開く」候補管理が導入されています"
        }

        val predicate = requireMethod("Lg;", "k", "Ljava/lang/Object;", listOf("Ljava/lang/Object;"))
        val longClick = requireMethod("Le4;", "onLongClick", "Z", listOf("Landroid/view/View;"))

        // b03 の候補フィルタ (g case 25) より前に、保存済みの非表示 ComponentName を除外する。
        predicate.addInstructionsWithLabels(
            0,
            """
                iget-byte v0, p0, Lg;->l:B
                const/16 v1, 0x19
                if-ne v0, v1, :original
                iget-object v0, p0, Lg;->m:Ljava/lang/Object;
                check-cast v0, Lb03;
                invoke-virtual {v0}, Lb03;->getContext()Landroid/content/Context;
                move-result-object v0
                check-cast p1, Ln8;
                invoke-interface {p1}, Ln8;->a()Landroid/content/ComponentName;
                move-result-object v1
                invoke-static {v0, v1}, $EXT->isVisible(Landroid/content/Context;Landroid/content/ComponentName;)Z
                move-result v0
                if-nez v0, :original
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """.trimIndent(),
            ExternalLabel("original", predicate.implementation!!.instructions.first())
        )

        // Berry 3.83.74 では case 18 の長押し処理が空。ここを候補管理 UI に割り当てる。
        longClick.addInstructionsWithLabels(
            0,
            """
                iget-byte v0, p0, Le4;->a:B
                const/16 v1, 0x12
                if-ne v0, v1, :original
                iget-object v0, p0, Le4;->c:Ljava/lang/Object;
                check-cast v0, Lz32;
                iget-object v1, p0, Le4;->b:Ljava/lang/Object;
                check-cast v1, Lgy;
                invoke-virtual {v1}, Lgy;->c()I
                move-result v1
                const/4 v2, -0x1
                if-eq v1, v2, :not_handled
                invoke-virtual {v0, v1}, Lz32;->N(I)Ljava/lang/Object;
                move-result-object v2
                check-cast v2, Ln8;
                invoke-interface {v2}, Ln8;->a()Landroid/content/ComponentName;
                move-result-object v2
                invoke-virtual {v0}, Lz32;->e()I
                move-result v3
                invoke-static {p1, v2, v3}, $EXT->onLongPress(Landroid/view/View;Landroid/content/ComponentName;I)Z
                move-result v1
                if-eqz v1, :not_handled
                iget-object v0, v0, Lz32;->q:Ljava/lang/Object;
                check-cast v0, Lb03;
                invoke-virtual {v0}, Lb03;->O()V
                const/4 v0, 0x1
                return v0
              :not_handled
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
            ExternalLabel("original", longClick.implementation!!.instructions.first())
        )
    }
}

private fun app.morphe.patcher.patch.BytecodePatchContext.requireMethod(
    classType: String,
    name: String,
    returnType: String,
    parameters: List<String>
): MutableMethod {
    val cls = mutableClassDefByOrNull(classType)
        ?: error("必要なクラスがありません: $classType")
    val matches = cls.methods.filter {
        it.name == name &&
        it.returnType == returnType &&
        it.parameters.map { p -> p.type } == parameters
    }
    require(matches.size == 1) {
        "対象メソッドが一意ではありません: $classType->$name"
    }
    require(matches[0].implementation != null) {
        "対象メソッドに実装がありません: $classType->$name"
    }
    return matches[0]
}
