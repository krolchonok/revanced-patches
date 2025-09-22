package app.revanced.patches.tiktok.misc.spoof.sim

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val spoofSimPatch = bytecodePatch(
    name = "SIM spoof",
    description = "Подмена информации, получаемой из SIM-карты.",
    use = true,
) {

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        val replacements = hashMapOf(
            "getSimCountryIso" to "getCountryIso",
            "getNetworkCountryIso" to "getCountryIso",
            "getSimOperator" to "getOperator",
            "getNetworkOperator" to "getOperator",
            "getSimOperatorName" to "getOperatorName",
            "getNetworkOperatorName" to "getOperatorName",
        )

        // какие значения ставим
        val spoofValues = mapOf(
            "getCountryIso" to "jp",         // MCC=257, MNC=01
            "getOperator" to "44010",        // numeric
            "getOperatorName" to "NTT DoCoMo"
        )

        // поиск всех вызовов TelephonyManager и подготовка патчей
        buildMap {
            classes.forEach { classDef ->
                val methods = classDef.methods
                buildMap methodList@{
                    methods.forEach methods@{ method ->
                        val impl = method.implementation?.instructions ?: return@methods
                        val patches = ArrayDeque<Pair<Int, String>>()
                        impl.forEachIndexed { index, insn ->
                            if (insn.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed
                            val ref = (insn as Instruction35c).reference as MethodReference
                            if (ref.definingClass != "Landroid/telephony/TelephonyManager;") return@forEachIndexed
                            replacements[ref.name]?.let { replacement ->
                                patches.add(index to replacement)
                            }
                        }
                        if (patches.isNotEmpty()) put(method, patches)
                    }
                }.also { if (it.isEmpty()) return@forEach }.let { methodPatches ->
                    put(classDef, methodPatches)
                }
            }
        }.forEach { (classDef, methods) ->
            with(proxy(classDef).mutableClass) {
                methods.forEach { (method, patches) ->
                    with(findMutableMethodOf(method)) {
                        while (patches.isNotEmpty()) {
                            val (invokeIndex, replacementKey) = patches.removeLast()

                            // сразу после invoke идёт move-result-object vA
                            val resultReg = getInstruction<OneRegisterInstruction>(invokeIndex + 1).registerA
                            val value = spoofValues.getValue(replacementKey) // гарантировано есть

                            // перезаписываем результат константой
                            addInstructions(
                                invokeIndex + 2,
                                """
                                    const-string v$resultReg, "$value"
                                """.trimIndent(),
                            )
                        }
                    }
                }
            }
        }
    }
}
