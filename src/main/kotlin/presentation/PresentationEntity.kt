package presentation

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity

/**
 * Subclass of [SceneEntity].
 * Enables entities to be revealed at different stages of a presentation by the [PresentationSystem].
 */
abstract class PresentationEntity : SceneEntity()
{
    /** At what slide index the entity should be revealed. */
    var revealOnSlideIndex = 0

    /** The current visibility value to render the entity with 0.0 = not visible, 1.0 = fully visible. */
    @JsonIgnore
    var visibility = 1f

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        if (visibility <= 0f)
            return // Invisible, don't draw to screen

        onDrawToScreen(engine, surface)
    }

    /**
     * Called when the entity should be visible on screen.
     */
    abstract fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
}