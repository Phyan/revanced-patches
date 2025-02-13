package app.revanced.patches.youtube.misc.splashanimation

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.splashanimation.fingerprints.WatchWhileActivityWithInFlagsFingerprint
import app.revanced.patches.youtube.misc.splashanimation.fingerprints.WatchWhileActivityWithOutFlagsFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.DarkSplashAnimation
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getWide32LiteralIndex
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(
    name = "Enable new splash animation",
    description = "Enables a new type of splash animation.",
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
object NewSplashAnimationPatch : BytecodePatch(
    setOf(
        WatchWhileActivityWithInFlagsFingerprint,
        WatchWhileActivityWithOutFlagsFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        WatchWhileActivityWithInFlagsFingerprint.result
            ?: WatchWhileActivityWithOutFlagsFingerprint.result
            ?: throw PatchException("Failed to resolve fingerprints")

        /**
         * ~YouTube v18.27.36
         */
        WatchWhileActivityWithInFlagsFingerprint.result?.let {
            it.mutableMethod.apply {
                var targetIndex = getWide32LiteralIndex(45407550) + 3
                if (getInstruction(targetIndex).opcode == Opcode.MOVE_RESULT)
                    targetIndex += 1

                inject(targetIndex)
            }
        }

        /**
         * YouTube v18.28.xx~
         */
        WatchWhileActivityWithOutFlagsFingerprint.result?.let {
            it.mutableMethod.apply {
                var startIndex = getWideLiteralIndex(DarkSplashAnimation) - 1
                val endIndex = startIndex - 30

                for (index in startIndex downTo endIndex) {
                    if (getInstruction(index).opcode != Opcode.IF_EQZ)
                        continue

                    startIndex = index - 8

                    arrayOf(
                        index,
                        index - 8
                    ).forEach { insertIndex -> inject(insertIndex) }

                    break
                }

                for (index in startIndex downTo endIndex) {
                    if (getInstruction(index).opcode != Opcode.IF_NE)
                        continue

                    inject(index)

                    break
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_NEW_SPLASH_ANIMATION"
            )
        )

        SettingsPatch.updatePatchStatus("Enable new splash animation")

    }

    private fun MutableMethod.inject(
        index: Int
    ) {
        if (getInstruction(index).opcode == Opcode.IF_NE)
            injectInt(index)
        else
            injectBoolean(index)
    }

    private fun MutableMethod.injectBoolean(index: Int) {
        val register = getInstruction<OneRegisterInstruction>(index).registerA

        addInstructions(
            index, """
                    invoke-static {v$register}, $MISC_PATH/SplashAnimationPatch;->enableNewSplashAnimationBoolean(Z)Z
                    move-result v$register
                    """
        )
    }

    private fun MutableMethod.injectInt(index: Int) {
        val register = getInstruction<TwoRegisterInstruction>(index).registerA

        addInstructions(
            index, """
                    invoke-static {v$register}, $MISC_PATH/SplashAnimationPatch;->enableNewSplashAnimationInt(I)I
                    move-result v$register
                    """
        )
    }
}
