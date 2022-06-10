package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.scene.SceneSystem
import no.njoh.pulseengine.modules.scene.entities.Camera

/**
 * This system is responsible for keeping track of the active "slide" and the user input related to changing slides.
 */
class PresentationSystem : SceneSystem()
{
    /** The index of the current slide. */
    var slideIndex = 0

    /** The upper limit of the slides. */
    var maxSlides = 100

    override fun onUpdate(engine: PulseEngine)
    {
        updateSlideIndex(engine)
        updateCamera(engine)
    }

    /**
     * Updates the [slideIndex] on key input.
     */
    private fun updateSlideIndex(engine: PulseEngine)
    {
        if (engine.input.wasClicked(Key.LEFT)) slideIndex--
        if (engine.input.wasClicked(Key.RIGHT) || engine.input.wasClicked(Key.SPACE)) slideIndex++

        // Limit the range to the highest available slide index
        slideIndex = slideIndex.coerceIn(0, maxSlides)
    }

    /**
     * Find the [CameraTarget] target with matching [slideIndex] and updates the [Camera] target values.
     */
    private fun updateCamera(engine: PulseEngine)
    {
        val camera = engine.scene.getFirstEntityOfType<Camera>() ?: return
        val target = engine.scene.getAllEntitiesOfType<CameraTarget>()
            ?.firstOrNull { it.slideIndex == slideIndex }
            ?: return

        camera.targetEntityId = target.id
        camera.targetZoom = target.zoom
    }
}