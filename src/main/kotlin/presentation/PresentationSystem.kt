package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.scene.SceneSystem
import no.njoh.pulseengine.modules.scene.entities.Camera
import tools.forEachEntityImplementing

/**
 * This system is responsible for keeping track of the active "slide" and the user input related to changing slides.
 */
class PresentationSystem : SceneSystem()
{
    /** The index of the current slide. */
    var slideIndex = 0

    /** The upper limit of the slides. */
    var maxSlides = 100

    /** The amount of milliseconds to reveal a [PresentationEntity]. */
    var revealTimeMs = 500f

    override fun onStart(engine: PulseEngine)
    {
        // Hide all entities that should be revealed later on in the presentation.
        engine.scene.forEachEntityImplementing<PresentationEntity>()
        {
            if (it.revealOnSlideIndex > slideIndex)
                it.visibility = 0f
        }
    }

    override fun onUpdate(engine: PulseEngine)
    {
        // Only track targets when scene is running
        if (engine.scene.state != SceneState.RUNNING)
            return

        updateSlideIndex(engine)
        updateCamera(engine)
        updateVisibilityOfPresentationEntities(engine)
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

    /**
     * Finds all [PresentationEntity]. and updates their visibility value based on the [slideIndex].
     */
    private fun updateVisibilityOfPresentationEntities(engine: PulseEngine)
    {
        val timeSinceLastUpdateMs = engine.data.deltaTime * 1000f
        val visibilityChangeRate = timeSinceLastUpdateMs / revealTimeMs.coerceAtLeast(1f)
        engine.scene.forEachEntityImplementing<PresentationEntity>()
        {
            if (slideIndex >= it.revealOnSlideIndex && it.visibility <= 1f)
                it.visibility = (it.visibility + visibilityChangeRate).coerceAtMost(1f)

            if (slideIndex < it.revealOnSlideIndex && it.visibility > 0f)
                it.visibility = (it.visibility - visibilityChangeRate).coerceAtLeast(0f)
        }
    }
}