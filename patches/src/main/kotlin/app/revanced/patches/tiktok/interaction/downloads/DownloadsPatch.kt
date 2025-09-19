package app.revanced.patches.tiktok.interaction.downloads

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val downloadsPatch = bytecodePatch(
    name = "Downloads",
    description = "Removes download restrictions and changes the default path to download to.",
) {
    dependsOn(
        sharedExtensionPatch,
    )

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        aclCommonShareFingerprint.method.replaceInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        aclCommonShare2Fingerprint.method.replaceInstructions(
            0,
            """
                const/4 v0, 0x2
                return v0
            """,
        )

        // Download videos without watermark.
        aclCommonShare3Fingerprint.method.addInstructionsWithLabels(
            0,
            """
                    invoke-static {}, Lapp/revanced/extension/tiktok/download/DownloadsPatch;->shouldRemoveWatermark()Z
                    move-result v0
                    if-eqz v0, :noremovewatermark
                    const/4 v0, 0x1
                    return v0
                    :noremovewatermark
                    nop
                """,
        )
    }
}