package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.scene.SceneSystem

/**
 * This system is responsible for keeping track of the active "slide" and the user input related to changing slides.
 */
class PresentationSystem : SceneSystem()
{
    /** The index of the current slide. */
    var slideIndex = 0

    /** The upper limit of the slides. */
    var maxSlides = 100

    /**
     * Updates the [slideIndex] on key input.
     */
    override fun onUpdate(engine: PulseEngine)
    {
        if (engine.input.wasClicked(Key.LEFT)) slideIndex--
        if (engine.input.wasClicked(Key.RIGHT) || engine.input.wasClicked(Key.SPACE)) slideIndex++

        // Limit the range to the highest available slide index
        slideIndex = slideIndex.coerceIn(0, maxSlides)
    }
}