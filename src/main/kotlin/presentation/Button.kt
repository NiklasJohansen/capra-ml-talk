package presentation

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.input.Mouse
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color
import presentation.Button.ButtonState.*
import presentation.Button.SwitchType.MOMENTARY
import presentation.Button.SwitchType.TOGGLE

/**
 * This entity is a button used to capture and forward messages about mouse or key presses.
 */
class Button : SceneEntity()
{
    /** ID of the entity to send a message to when the [ButtonState] changes. */
    var targetEntityId = -1L

    /** The message to dispatch when the button is pressed. */
    var pressedEventMessage = "PRESSED"

    /** The message to dispatch when the button is released. */
    var releasedEventMessage = "RELEASED"

    /** Determines how the button switches state when pressed. */
    var switchType = MOMENTARY

    /** Will trigger the button on key presses when set true. */
    var triggerOnKeyPress = false

    /** The keyboard-key to listen for key-presses on. */
    var triggerKey = Key.K_1

    /** The relevant slide index to listen for key-presses. Set to -1 if all slides are relevant. */
    var triggerKeySlideIndex = -1

    // Styling parameters
    var releasedTextureName = ""
    var pressedTextureName = ""
    var releasedColor = Color(0.8f, 0.8f, 0.8f)
    var hoverColor = Color(0.9f, 0.9f, 0.9f)
    var pressedColor = Color(1.0f, 1.0f, 1.0f)
    var cornerRadius = 10f

    @JsonIgnore
    private var state: ButtonState = RELEASED

    /**
     * Determines the button state based on mouse and keyboard input.
     */
    override fun onUpdate(engine: PulseEngine)
    {
        val xm = engine.input.xWorldMouse
        val ym = engine.input.yWorldMouse
        val xLeft = x - width * 0.5f
        val yTop = y - height * 0.5f
        val isMouseInsideButton = (xm > xLeft && xm < xLeft + width && ym > yTop && ym < yTop + height)
        val isMousePressed = engine.input.isPressed(Mouse.LEFT)
        val isMouseClicked = engine.input.wasClicked(Mouse.LEFT)
        val checkKey = (triggerOnKeyPress && isRelevantSlideIndex(engine))

        val newState = when (switchType)
        {
            MOMENTARY ->
            {
                if (checkKey && engine.input.isPressed(triggerKey))
                {
                    PRESSED
                }
                else if (isMouseInsideButton)
                {
                    if (isMousePressed) PRESSED else HOVER
                }
                else RELEASED
            }
            TOGGLE ->
            {
                if ((checkKey && engine.input.wasClicked(triggerKey)) || (isMouseInsideButton && isMouseClicked))
                {
                    if (state == PRESSED) HOVER else PRESSED
                }
                else if (state != PRESSED)
                {
                    if (isMouseInsideButton) HOVER else RELEASED
                }
                else state
            }
        }

        setButtonState(engine, newState)
    }

    /**
     * Draw the button to screen.
     */
    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val color = when (state)
        {
            RELEASED -> releasedColor
            HOVER -> hoverColor
            PRESSED -> pressedColor
        }

        val textureName = when (state)
        {
            RELEASED, HOVER -> releasedTextureName
            PRESSED -> pressedTextureName
        }

        val texture = engine.asset.getOrNull(textureName) ?: Texture.BLANK

        surface.setDrawColor(color)
        surface.drawTexture(texture, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = cornerRadius)
    }

    /**
     * Updates the [state] if the [newState] differs from the current [state].
     * Notifies the target entity with the appropriate event message on state change.
     */
    private fun setButtonState(engine: PulseEngine, newState: ButtonState)
    {
        val lastState = state
        if (newState == lastState)
            return // No state change

        // Update state
        state = newState

        // Determine message to send and notify listener
        val eventMessage = when
        {
            newState == PRESSED -> pressedEventMessage
            lastState == PRESSED -> releasedEventMessage
            else -> return
        }
        engine.scene.getEntityOfType<EventListener>(targetEntityId)?.handleEvent(engine, eventMessage)
    }

    /**
     * Returns true if the current slide index matches the [triggerKeySlideIndex].
     */
    private fun isRelevantSlideIndex(engine: PulseEngine): Boolean
    {
        val slideIndex = engine.scene.getSystemOfType<PresentationSystem>()?.slideIndex ?: return true
        return slideIndex == triggerKeySlideIndex
    }

    /**
     * The finite states a [Button] can have.
     */
    enum class ButtonState
    {
        RELEASED, HOVER, PRESSED;
    }

    enum class SwitchType
    {
        /** State changes when button is hold down and then back again when released. */
        MOMENTARY,
        /** Click once to change state, click again to change back. */
        TOGGLE
    }
}

