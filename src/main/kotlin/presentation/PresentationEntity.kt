package presentation

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.surface.Surface
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.modules.scene.entities.StandardSceneEntity
import tools.animate
import tools.getEventValue
import kotlin.math.max

/**
 * Subclass of [SceneEntity] that provides functionality to change the entity visibility via event messages.
 */
abstract class PresentationEntity : StandardSceneEntity(),  EventListener
{
    /** The initial visibility value at the start of the scene in range (0.0 - 1.0). */
    var initialVisibility = 1f

    /** The current visibility value to render the entity with 0.0 = not visible, 1.0 = fully visible. */
    @JsonIgnore var visibility = 1f

    override fun onStart(engine: PulseEngine)
    {
        visibility = initialVisibility
    }

    override fun onRender(engine: PulseEngine, surface: Surface)
    {
        if (visibility <= 0f || !isOnScreen(surface))
            return // Invisible, don't draw to screen

        onDrawToScreen(engine, surface)
    }

    override fun onFixedUpdate(engine: PulseEngine) { }

    private fun isOnScreen(surface: Surface): Boolean
    {
        val topLeft = surface.camera.topLeftWorldPosition
        val bottomRight = surface.camera.bottomRightWorldPosition
        val size = max(width, height) * 0.5f
        return x + size > topLeft.x && x - size < bottomRight.x && y + size > topLeft.y && y - size < bottomRight.y
    }

    /**
     * Handles visibility and depth related events or forwards event down to concrete implementation.
     */
    override fun handleEvent(engine: PulseEngine, eventMsg: String)
    {
        when
        {
            eventMsg == "SHOW" -> engine.animate(::visibility, target = 1f)
            eventMsg == "HIDE" -> engine.animate(::visibility, target = 0f)
            eventMsg.startsWith("FADE_") -> eventMsg.getEventValue()?.let { engine.animate(::visibility, it) }
            eventMsg.startsWith("DEPTH_") -> eventMsg.getEventValue()?.let { engine.animate(::z, it) }
            else -> onEventMessage(engine, eventMsg)
        }
    }

    /**
     * Called when the entity should be visible on screen.
     */
    abstract fun onDrawToScreen(engine: PulseEngine, surface: Surface)

    /**
     * Called when this entity is sent an event message.
     */
    open fun onEventMessage(engine: PulseEngine, eventMessage: String) {  }
}