package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.scene.SceneSystem
import no.njoh.pulseengine.modules.scene.entities.Camera
import kotlin.math.max

/**
 * This system is responsible for keeping track of the active "slide" and the user input related to changing slides.
 */
class PresentationSystem : SceneSystem()
{
    /** The index of the current slide. */
    var slideIndex = 0

    override fun onUpdate(engine: PulseEngine)
    {
        // Only track targets when scene is running
        if (engine.scene.state != SceneState.RUNNING)
            return

        // Find the camera target with matching "slide index" and update the cameras target values
        var highestSlideIndex = 0
        engine.scene.forEachEntityOfType<CameraTarget>
        {
            if (it.slideIndex == slideIndex)
            {
                engine.scene.getFirstEntityOfType<Camera>()?.apply()
                {
                    targetEntityId = it.id
                    targetZoom = it.zoom
                }
            }

            highestSlideIndex = max(it.slideIndex, highestSlideIndex)
        }

        // Update slide index on key input
        if (engine.input.wasClicked(Key.LEFT)) slideIndex--
        if (engine.input.wasClicked(Key.RIGHT) || engine.input.wasClicked(Key.SPACE)) slideIndex++

        // Limit the range to the highest available slide index
        slideIndex = slideIndex.coerceIn(0, highestSlideIndex)
    }
}