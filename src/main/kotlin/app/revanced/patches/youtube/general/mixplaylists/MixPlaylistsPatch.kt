package app.revanced.patches.youtube.general.mixplaylists

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.general.mixplaylists.fingerprints.BottomPanelOverlayTextFingerprint
import app.revanced.patches.youtube.general.mixplaylists.fingerprints.ElementParserFingerprint
import app.revanced.patches.youtube.general.mixplaylists.fingerprints.EmptyFlatBufferFingerprint
import app.revanced.patches.youtube.utils.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import app.revanced.util.integrations.Constants.GENERAL
import app.revanced.util.integrations.Constants.PATCHES_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(
    name = "Hide mix playlists",
    description = "Hides mix playlists in feed.",
    dependencies = [
        LithoFilterPatch::class,
        SettingsPatch::class
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
object MixPlaylistsPatch : BytecodePatch(
    setOf(
        BottomPanelOverlayTextFingerprint,
        ElementParserFingerprint,
        EmptyFlatBufferFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Hide MixPlaylists when tablet UI is turned on
         * Required only for RVX Patches
         */
        BottomPanelOverlayTextFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $GENERAL->hideMixPlaylists(Landroid/view/View;)V"
                )
            }
        } ?: throw BottomPanelOverlayTextFingerprint.exception

        /**
         * Separated from bytebuffer patch
         * Target method is only used for Hide MixPlaylists patch
         */
        ElementParserFingerprint.result
            ?: EmptyFlatBufferFingerprint.result
            ?: throw EmptyFlatBufferFingerprint.exception


        /**
         * ~ YouTube v18.29.38
         */
        EmptyFlatBufferFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.CHECK_CAST
                } + 1
                val jumpIndex = getStringIndex("Failed to convert Element to Flatbuffers: %s") + 2

                val freeIndex = it.scanResult.patternScanResult!!.startIndex - 1

                inject(freeIndex, insertIndex, jumpIndex)
            }
        }

        /**
         * YouTube v18.30.xx~
         */
        ElementParserFingerprint.result?.let {
            it.mutableMethod.apply {
                val methodInstructions = implementation!!.instructions

                val insertIndex = methodInstructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.INVOKE_INTERFACE
                            && (instruction as? ReferenceInstruction)?.reference.toString()
                        .contains("[B")
                }
                val freeIndex = it.scanResult.patternScanResult!!.startIndex - 1

                for (index in methodInstructions.size - 1 downTo 0) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_INTERFACE_RANGE) continue

                    val jumpIndex = index + 1

                    inject(freeIndex, insertIndex, jumpIndex)

                    break
                }
            }
        }

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_MIX_PLAYLISTS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide mix playlists")

    }

    private const val FILTER_CLASS_DESCRIPTOR =
        "$PATCHES_PATH/ads/MixPlaylistsFilter;"

    private fun MutableMethod.inject(
        freeIndex: Int,
        insertIndex: Int,
        jumpIndex: Int
    ) {
        val freeRegister = getInstruction<TwoRegisterInstruction>(freeIndex).registerA
        val objectIndex = implementation!!.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.MOVE_OBJECT
        }
        val objectRegister = getInstruction<TwoRegisterInstruction>(objectIndex).registerA
        addInstructionsWithLabels(
            insertIndex, """
                invoke-static {v$objectRegister, v$freeRegister}, $FILTER_CLASS_DESCRIPTOR->filterMixPlaylists(Ljava/lang/Object;[B)Z
                move-result v$freeRegister
                if-nez v$freeRegister, :not_an_ad
                """, ExternalLabel("not_an_ad", getInstruction(jumpIndex))
        )

        addInstruction(
            0,
            "move-object/from16 v$freeRegister, p3"
        )
    }
}
