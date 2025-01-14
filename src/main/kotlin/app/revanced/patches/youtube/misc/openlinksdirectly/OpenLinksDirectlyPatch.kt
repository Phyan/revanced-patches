package app.revanced.patches.youtube.misc.openlinksdirectly

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.misc.openlinksdirectly.fingerprints.OpenLinksDirectlyFingerprintPrimary
import app.revanced.patches.youtube.misc.openlinksdirectly.fingerprints.OpenLinksDirectlyFingerprintSecondary
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Patch(
    name = "Enable open links directly",
    description = "Skips over redirection URLs to external links.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41"
            ]
        )
    ]
)
@Suppress("unused")
object OpenLinksDirectlyPatch : BytecodePatch(
    setOf(
        OpenLinksDirectlyFingerprintPrimary,
        OpenLinksDirectlyFingerprintSecondary
    )
) {
    override fun execute(context: BytecodeContext) {

        arrayOf(
            OpenLinksDirectlyFingerprintPrimary,
            OpenLinksDirectlyFingerprintSecondary
        ).forEach { fingerprint ->
            fingerprint.result?.let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex
                    val register = getInstruction<Instruction35c>(insertIndex).registerC

                    replaceInstruction(
                        insertIndex,
                        "invoke-static {v$register}, $MISC_PATH/OpenLinksDirectlyPatch;->enableBypassRedirect(Ljava/lang/String;)Landroid/net/Uri;"
                    )
                }
            } ?: throw fingerprint.exception
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_OPEN_LINKS_DIRECTLY"
            )
        )

        SettingsPatch.updatePatchStatus("Enable open links directly")

    }
}
