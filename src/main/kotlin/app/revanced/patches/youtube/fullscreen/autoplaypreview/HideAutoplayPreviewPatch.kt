package app.revanced.patches.youtube.fullscreen.autoplaypreview

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AutoNavPreviewStub
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AutoNavToggle
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.FULLSCREEN
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide autoplay preview",
    description = "Hides the autoplay preview container in the fullscreen.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
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
object HideAutoplayPreviewPatch : BytecodePatch(
    setOf(LayoutConstructorFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        LayoutConstructorFingerprint.result?.let {
            it.mutableMethod.apply {
                val dummyRegister =
                    getInstruction<OneRegisterInstruction>(getStringIndex("1.0x")).registerA
                val insertIndex = getWideLiteralIndex(AutoNavPreviewStub)
                val jumpIndex = getWideLiteralIndex(AutoNavToggle) - 1

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $FULLSCREEN->hideAutoPlayPreview()Z
                        move-result v$dummyRegister
                        if-nez v$dummyRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
            }
        } ?: throw LayoutConstructorFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_AUTOPLAY_PREVIEW"
            )
        )

        SettingsPatch.updatePatchStatus("Hide autoplay preview")

    }
}

