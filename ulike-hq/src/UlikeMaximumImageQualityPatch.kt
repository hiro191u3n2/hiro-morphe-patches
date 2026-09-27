package app.hiro.ulike.hq

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val MAX_PICTURE_WIDTH = 4096L

private val target = Compatibility(
    packageName = "com.gorgeous.liteinternational",
    name = "Ulike",
    description = "5.6.2 (740) の未改造APK専用",
    targets = listOf(AppTarget("5.6.2", 740))
)

@Suppress("unused")
val ulikeMaximumImageQualityPatch = bytecodePatch(
    name = "撮影画質を最大化（4096・原画像バッファ制限解除）",
    description = "Ulike 5.6.2 (740) の撮影上限を4096pxへ統一し、1920/2560/3264系の幅制限、原画像バッファの360px制限、ShotScreen側の360px制限を解除します。Max Width Take Pictureも常時有効化します。JPEG品質は元から100のため変更しません。未改造APK専用です。",
    default = true
) {
    compatibleWith(target)

    execute {
        require(packageMetadata.packageName == "com.gorgeous.liteinternational" &&
                packageMetadata.versionName == "5.6.2" &&
                packageMetadata.versionCode.toString() == "740") {
            "Ulike 5.6.2 (740) の未改造APKを選択してください"
        }

        listOf("d", "o", "p").forEach { methodName ->
            val method = requireMethod("Li/f/l/o/f/o/s;", methodName, "I", emptyList())
            val instructions = method.implementation!!.instructions
            require(instructions.size == 2 && instructions[0].opcode == Opcode.SGET &&
                    instructions[1].opcode == Opcode.RETURN) {
                "Ulikeの撮影幅アクセサ ($methodName) が想定と異なります"
            }
            val register = (instructions[0] as OneRegisterInstruction).registerA
            method.replaceInstruction(0, "const/16 v$register, 0x1000")
        }

        requireMethod("Li/f/l/r/j;", "b", "I", listOf("Z"))
            .replaceSingleConst16(1920L, MAX_PICTURE_WIDTH)

        requireMethod(
            "Li/f/l/r/t;",
            "a",
            "V",
            listOf(
                "I", "Z", "Z",
                "Lcom/ss/android/vesdk/VERecorder\$ILightSoftCallback;",
                "Lcom/ss/android/vesdk/VERecorder\$IBitmapCaptureCallback;",
                "Z", "Z",
                "Lcom/ss/android/vesdk/VERecorder\$IVEFrameShotScreenCallback;"
            )
        ).replaceSingleConst16(360L, MAX_PICTURE_WIDTH)

        requireMethod(
            "Li/f/l/r/t;",
            "e",
            "V",
            listOf(
                "I", "I", "Z",
                "Lcom/ss/android/vesdk/VERecorder\$IBitmapShotScreenCallback;",
                "Lcom/ss/android/vesdk/VERecorder\$IVEFrameShotScreenCallback;",
                "Z", "Z", "Z"
            )
        ).replaceSingleConst16(360L, MAX_PICTURE_WIDTH)

        requireMethod(
            "Li/f/l/r/v;",
            "c",
            "V",
            listOf("Lcom/ss/android/vesdk/VERecorder\$IBitmapShotScreenCallback;")
        ).apply {
            val matches = implementation!!.instructions.withIndex()
                .filter { (_, instruction) ->
                    (instruction as? WideLiteralInstruction)?.wideLiteral == 360L
                }
            require(matches.size == 2) {
                "ShotScreenの360px制限が2か所見つかりません"
            }

            matches.forEach { (index, instruction) ->
                when (instruction.opcode) {
                    Opcode.MUL_INT_LIT16 -> {
                        val two = instruction as TwoRegisterInstruction
                        replaceInstruction(
                            index,
                            "mul-int/lit16 v${two.registerA}, v${two.registerB}, 0x1000"
                        )
                    }
                    Opcode.CONST_16 -> {
                        val one = instruction as OneRegisterInstruction
                        replaceInstruction(index, "const/16 v${one.registerA}, 0x1000")
                    }
                    else -> error("ShotScreenの360px制限命令が想定外です: ${instruction.opcode}")
                }
            }
        }

        requireMethod(
            "Li/f/l/u/i;",
            "b",
            "Lcom/ss/android/vesdk/VECameraSettings;",
            listOf("Li/f/l/u/g;", "I")
        ).apply {
            val instructions = implementation!!.instructions

            val settingsStart = findMethodCall(
                definingClass = "Li/f/l/u/g;",
                name = "s",
                returnType = "Li/f/l/u/o;",
                parameters = emptyList()
            )
            val booleanValue = instructions.withIndex().firstOrNull { (index, instruction) ->
                index > settingsStart &&
                    instruction.methodReferenceMatches(
                        "Ljava/lang/Boolean;",
                        "booleanValue",
                        "Z",
                        emptyList()
                    )
            }?.index ?: error("Max Width Take Picture設定のbooleanValueが見つかりません")
            require(booleanValue - settingsStart < 12) {
                "Max Width Take Picture設定の命令位置が想定と異なります"
            }

            val featureFlag = findMethodCall(
                definingClass = "Li/f/j0/d/d;",
                name = "L",
                returnType = "Z",
                parameters = emptyList()
            )
            val setUseMaxWidth = findMethodCall(
                definingClass = "Lcom/ss/android/vesdk/VECameraSettings\$Builder;",
                name = "setUseMaxWidthTakePicture",
                returnType = "Lcom/ss/android/vesdk/VECameraSettings\$Builder;",
                parameters = listOf("Z"),
                startIndex = featureFlag
            )
            require(setUseMaxWidth > featureFlag) {
                "setUseMaxWidthTakePictureの呼出し位置が想定と異なります"
            }

            forceMoveResult(booleanValue + 1, 1)
            forceMoveResult(featureFlag + 1, 0)
        }
    }
}

private fun MutableMethod.replaceSingleConst16(oldValue: Long, newValue: Long) {
    val matches = implementation!!.instructions.withIndex()
        .filter { (_, instruction) ->
            instruction.opcode == Opcode.CONST_16 &&
                (instruction as? WideLiteralInstruction)?.wideLiteral == oldValue
        }
    require(matches.size == 1) {
        "const/16 $oldValue が1か所に特定できません: $definingClass->$name"
    }
    val (index, instruction) = matches.single()
    val register = (instruction as OneRegisterInstruction).registerA
    replaceInstruction(index, "const/16 v$register, 0x${newValue.toString(16)}")
}

private fun MutableMethod.forceMoveResult(index: Int, value: Int) {
    val instruction = implementation!!.instructions.getOrNull(index)
        ?: error("move-resultの位置が範囲外です: $definingClass->$name")
    require(instruction.opcode == Opcode.MOVE_RESULT) {
        "想定したmove-resultがありません: $definingClass->$name index=$index"
    }
    val register = (instruction as OneRegisterInstruction).registerA
    replaceInstruction(index, "const/4 v$register, $value")
}

private fun MutableMethod.findMethodCall(
    definingClass: String,
    name: String,
    returnType: String,
    parameters: List<String>,
    startIndex: Int = 0
): Int {
    val matches = implementation!!.instructions.withIndex().filter { (index, instruction) ->
        index >= startIndex &&
            instruction.methodReferenceMatches(definingClass, name, returnType, parameters)
    }
    require(matches.size == 1) {
        "対象メソッド呼出しを一意に特定できません: $definingClass->$name"
    }
    return matches.single().index
}

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.methodReferenceMatches(
    definingClass: String,
    name: String,
    returnType: String,
    parameters: List<String>
): Boolean {
    val reference = (this as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return reference.definingClass == definingClass &&
        reference.name == name &&
        reference.returnType == returnType &&
        reference.parameterTypes.map { it.toString() } == parameters
}

private fun BytecodePatchContext.requireMethod(
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
