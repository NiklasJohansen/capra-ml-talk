package tools

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.scene.SceneSystem
import presentation.SlideTrigger

/**
 * System to provide general tools for working with [SceneEntity]s in editor.
 */
class EntityTool : SceneSystem()
{
    /** The key to press for copying the IDs of all selected entities to clipboard. */
    var copyEntityIdsKey = Key.I

    override fun onUpdate(engine: PulseEngine)
    {
        if (engine.scene.state != SceneState.STOPPED)
            return // Tool is only active in editor

        handleEntityIdCopying(engine)
        handleSlideTrigger(engine)
    }

    /**
     * Handles copying of the IDs for selected entities to clipboard.
     */
    private fun handleEntityIdCopying(engine: PulseEngine)
    {
        if (!engine.input.wasClicked(copyEntityIdsKey))
            return // Key was not pressed, return

        val builder = StringBuilder()
        engine.scene.forEachEntity()
        {
            if (it.isSet(SceneEntity.SELECTED))
            {
                builder.append(it.id)
                builder.append(',')
            }
        }

        if (builder.isNotBlank())
            engine.input.setClipboard(builder.removeSuffix(",").toString())
    }

    /**
     * Handles updating of target slide index for all selected [SlideTrigger] entities.
     */
    private fun handleSlideTrigger(engine: PulseEngine)
    {
        if (engine.scene.state != SceneState.STOPPED)
            return // Should only handle key input in editor, return

        var change = 0
        if (engine.input.wasClicked(Key.KP_ADD)) change++
        if (engine.input.wasClicked(Key.KP_SUBTRACT)) change--
        if (change == 0)
            return // No key pressed, return

        engine.scene.forEachEntityOfType<SlideTrigger>()
        {
            if (it.isSet(SceneEntity.SELECTED)) it.targetSlideIndex += change
        }
    }
}