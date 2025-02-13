package app.revanced.patches.youtube.general.autopopuppanels

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.general.autopopuppanels.fingerprints.EngagementPanelControllerFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL

@Patch(
    name = "Hide auto player popup panels",
    description = "Hide automatic popup panels (playlist or live chat) on video player.",
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
object PlayerPopupPanelsPatch : BytecodePatch(
    setOf(EngagementPanelControllerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        EngagementPanelControllerFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $GENERAL->hideAutoPlayerPopupPanels()Z
                        move-result v0
                        if-eqz v0, :player_popup_panels_shown
                        if-eqz p4, :player_popup_panels_shown
                        const/4 v0, 0x0
                        return-object v0
                        """, ExternalLabel("player_popup_panels_shown", getInstruction(0))
                )
            }
        } ?: throw EngagementPanelControllerFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_AUTO_PLAYER_POPUP_PANELS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide auto player popup panels")

    }
}
