package presentation

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import tools.animate

/**
 * Subclass of [SceneEntity].
 * Enables entities to be revealed at different stages of a presentation by the [PresentationSystem].
 */
abstract class PresentationEntity : SceneEntity(), EventListener
{
    /** The initial visibility value at the start of the scene in range (0.0 - 1.0). */
    var initialVisibility = 1f

    /** The current visibility value to render the entity with 0.0 = not visible, 1.0 = fully visible. */
    @JsonIgnore
    var visibility = 1f

    override fun onStart(engine: PulseEngine)
    {
        visibility = initialVisibility
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        if (visibility <= 0f)
            return // Invisible, don't draw to screen

        onDrawToScreen(engine, surface)
    }

    /**
     * Handles visibility related events or forwards event down to concrete implementation.
     */
    override fun handleEvent(engine: PulseEngine, eventMessage: String)
    {
        when (eventMessage)
        {
            "SHOW" -> engine.animate(from = visibility, to = 1f) { visibility = it }
            "HIDE" -> engine.animate(from = visibility, to = 0f) { visibility = it }
            else -> onEventMessage(engine, eventMessage)
        }
    }

    /**
     * Called when the entity should be visible on screen.
     */
    abstract fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)

    /**
     * Called when this entity is sent an event message.
     */
    open fun onEventMessage(engine: PulseEngine, eventMessage: String) {  }
}