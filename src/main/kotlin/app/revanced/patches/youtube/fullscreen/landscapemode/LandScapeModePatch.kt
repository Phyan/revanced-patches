package app.revanced.patches.youtube.fullscreen.landscapemode

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints.OrientationParentFingerprint
import app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints.OrientationPrimaryFingerprint
import app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints.OrientationSecondaryFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Disable landscape mode",
    description = "Disable landscape mode when entering fullscreen.",
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
    ],
    use = false
)
@Suppress("unused")
object LandScapeModePatch : BytecodePatch(
    setOf(OrientationParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        OrientationParentFingerprint.result?.classDef?.let { classDef ->
            arrayOf(
                OrientationPrimaryFingerprint,
                OrientationSecondaryFingerprint
            ).forEach {
                it.also { it.resolve(context, classDef) }.result?.injectOverride()
                    ?: throw it.exception
            }
        } ?: throw OrientationParentFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: DISABLE_LANDSCAPE_MODE"
            )
        )

        SettingsPatch.updatePatchStatus("Disable landscape mode")

    }

    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$FULLSCREEN->disableLandScapeMode(Z)Z"

    private fun MethodFingerprintResult.injectOverride() {
        mutableMethod.apply {
            val index = scanResult.patternScanResult!!.endIndex
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                        invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR
                        move-result v$register
                        """
            )
        }
    }
}
