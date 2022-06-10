package presentation

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.SceneState

/**
 * Responsible for dispatching event messages when the presentation transition from and to a certain slide index.
 * Messages are dispatched to a one or more entities of type [EventListener].
 */
class SlideTrigger : SceneEntity()
{
    /** The name of this trigger. Only visible in editor. */
    var name = "Slide Trigger"

    /** The target slide index that should trigger an event dispatch. */
    var targetSlideIndex = 0

    /** The IDs of the entities to receive the event message. */
    var targetEntityIds = ""

    /** The message to dispatch when the presentation enters the [targetSlideIndex]. */
    var onEnterMessage = ""

    /** The message to dispatch when the presentation enters the next slide index after the [targetSlideIndex]. */
    var onNextMessage = ""

    /** The message to dispatch when the presentation enters the previous slide index before the [targetSlideIndex]. */
    var onPreviousMessage = ""

    @JsonIgnore private var lastMessage: String? = null
    @JsonIgnore private var lastSlideIndex = -1

    override fun onUpdate(engine: PulseEngine)
    {
        handleEventDispatch(engine)
    }

    /**
     * Handles dispatching of event messages to target entities when the presentation slide index changes.
     */
    private fun handleEventDispatch(engine: PulseEngine)
    {
        val slideIndex = engine.scene.getSystemOfType<PresentationSystem>()?.slideIndex ?: return
        val message = when
        {
            slideIndex == lastSlideIndex -> return // Nothing changed, return
            slideIndex == targetSlideIndex -> onEnterMessage
            slideIndex > targetSlideIndex && lastSlideIndex == targetSlideIndex -> onNextMessage
            slideIndex < targetSlideIndex && lastSlideIndex == targetSlideIndex -> onPreviousMessage
            else -> null
        }

        if (message != null && message != lastMessage && targetEntityIds.isNotBlank())
        {
            // Send message to all target entities
            for (id in targetEntityIds.toLongIds())
                engine.scene.getEntityOfType<EventListener>(id)?.handleEvent(engine, message)
        }

        lastMessage = message
        lastSlideIndex = slideIndex
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        if (engine.scene.state != SceneState.STOPPED)
            return // Only draw Trigger in editor

        val entityIdString = "${targetEntityIds.take(15)} ${if(targetEntityIds.length > 15) "..." else ""}"

        surface.setDrawColor(0.1f,0.1f, 0.1f)
        surface.drawTexture(Texture.BLANK, x, y, width + 2, height + 2, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = 5f)
        surface.setDrawColor(0.9f,0.8f, 0.8f)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = 5f)
        surface.setDrawColor(0f,0f, 0f)
        surface.drawText("($targetSlideIndex) $name", x, y - 20f, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 15f)
        surface.drawText("Entity IDs: $entityIdString", x, y - 5, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 10f)
        surface.drawText("OnEnter: $onEnterMessage", x, y + 5, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 10f)
        surface.drawText("OnPrev: $onPreviousMessage", x, y + 15, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 10f)
        surface.drawText("OnNext: $onNextMessage", x, y + 25, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 10f)
    }

    /** Returns a list of [Long] values from a comma-separated [String]. */
    private fun String.toLongIds() = this.split(",").mapNotNull { it.trim().toLongOrNull() }
}