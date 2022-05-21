package tools

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.CursorType
import no.njoh.pulseengine.core.input.Mouse
import no.njoh.pulseengine.core.shared.primitives.Color
import java.util.*
import kotlin.math.max

/**
 * Returns a formatted string representation of a [Float] value.
 * Example: 0.123445 -> "0.12"
 */
fun Float.format() = String.format(Locale.ENGLISH, "%.2f", this)

/**
 * Util function to multiply the RGB values of a [Color] object by a factor [v].
 * Returns a new instance of the [Color] object.
 */
operator fun Color.times(v: Float) = Color(red * v, green * v, blue * v, alpha)

/**
 * Utility function to update a float value of a target entity using mouse input.
 */
fun editEntityValue(engine: PulseEngine, entityId: Long): Float
{
    val e = engine.scene.getEntity(entityId) ?: return 0f
    val xm = engine.input.xWorldMouse
    val ym = engine.input.yWorldMouse
    val radius = max(e.width, e.height) * 0.5f
    val isMouseInsideEntity = (xm - e.x) * (xm - e.x) + (ym - e.y) * (ym - e.y) < radius * radius

    if (isMouseInsideEntity)
        engine.input.setCursor(CursorType.VERTICAL_RESIZE)

    if (engine.input.isPressed(Mouse.LEFT))
    {
        if (isMouseInsideEntity && selectedEntity == null)
            selectedEntity = entityId

        if (selectedEntity == e.id)
            return engine.input.ydMouse * -0.01f
    }
    else selectedEntity = null

    return 0f
}

/** Stores the ID of the selected entity */
var selectedEntity: Long? = null