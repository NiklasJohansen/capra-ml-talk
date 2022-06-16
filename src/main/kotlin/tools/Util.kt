package tools

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.input.CursorType
import no.njoh.pulseengine.core.input.Mouse
import no.njoh.pulseengine.core.scene.SceneManager
import no.njoh.pulseengine.core.shared.primitives.Color
import presentation.Animator
import presentation.Animator.EasingFunction
import java.util.*
import kotlin.math.max
import kotlin.reflect.KMutableProperty

/**
 * Returns a formatted string representation of a [Float] value.
 * Example: 0.123445 -> "0.12"
 */
fun Float.format() = String.format(Locale.ENGLISH, "%.2f", this)

/**
 * Linear interpolates between [a] and [b] with value [v] in range 0.0 - 1.0.
 */
fun lerp(a: Float, b: Float, v: Float) = a * (1f - v) + b * v

/**
 * Shorthand function to set the draw color of a [Surface2D] along with a given alpha value (transparency).
 */
fun Surface2D.setDrawColor(color: Color, alpha: Float) =
    setDrawColor(color.red, color.green, color.blue, color.alpha * alpha)

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

/**
 * Returns the next pseudorandom, Gaussian ("normally") distributed float value
 * with mean 0.0 and standard deviation 1.0
 */
fun nextRandomGaussian() = random.nextGaussian().toFloat()
private val random = Random() // java.util.Random provides .nextGaussian() function

/**
 * Util function to loop through all scene entities implementing type [T].
 */
inline fun <reified T> SceneManager.forEachEntityImplementing(action: (T) -> Unit)
{
    this.forEachEntityTypeList { list ->
        // Each list contains entities of a single subclass of SceneEntity.
        // So if the first entity is of type T, all entities in the list will be of type T.
        if (list[0] is T)
            list.forEachFast { action(it as T) }
    }
}

/**
 * Global function for using the [Animator] system to animate properties.
 * @param property a mutable property to be animated
 * @param target the target value the animation will end on
 * @param durationMs the duration of the animation in milliseconds
 * @param easingFunc the function to use when animating the property to the target value
 */
fun PulseEngine.animate(property: KMutableProperty<Float>, target: Float, durationMs: Long? = null, easingFunc: EasingFunction? = null)
{
    scene.getSystemOfType<Animator>()
        ?.addAnimation(property, target, durationMs, easingFunc)
        ?: property.setter.call(target)
}