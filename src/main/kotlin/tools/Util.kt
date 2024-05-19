package tools

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.surface.Surface
import no.njoh.pulseengine.core.input.CursorType
import no.njoh.pulseengine.core.input.MouseButton
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.interfaces.Spatial
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
fun Float.format(): String
{
    val string = String.format(Locale.ENGLISH, "%.2f", this)
    return if (string == "-0.00") "0.00" else string
}

/**
 * Linear interpolates between [a] and [b] with value [v] in range 0.0 - 1.0.
 */
fun lerp(a: Float, b: Float, v: Float) = a * (1f - v) + b * v

/**
 * Shorthand function to set the draw color of a [Surface] along with a given alpha value (transparency).
 */
fun Surface.setDrawColor(color: Color, alpha: Float) =
    setDrawColor(color.red, color.green, color.blue, color.alpha * alpha)

/**
 * Utility function to update a float value of a target entity using mouse input.
 */
fun editEntityValue(engine: PulseEngine, entityId: Long): Float
{
    val e = engine.scene.getEntityOfType<Spatial>(entityId) ?: return 0f
    val xm = engine.input.xWorldMouse
    val ym = engine.input.yWorldMouse
    val radius = max(e.width, e.height) * 0.5f
    val isMouseInsideEntity = (xm - e.x) * (xm - e.x) + (ym - e.y) * (ym - e.y) < radius * radius

    if (isMouseInsideEntity)
        engine.input.setCursorType(CursorType.VERTICAL_RESIZE)

    if (engine.input.isPressed(MouseButton.LEFT))
    {
        if (isMouseInsideEntity && selectedEntity == null)
            selectedEntity = entityId

        if (selectedEntity == (e as SceneEntity).id)
            return (engine.input.ydMouse / 2).toInt() * -0.01f
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

/**
 * Maps a [List] to an [Array].
 */
inline fun <reified T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> =
    Array(this.size) { i -> transform(this[i]) }

/**
 * Maps a [List] to a [LongArray].
 */
inline fun <reified T> List<T>.mapToLongArray(transform: (T) -> Long): LongArray =
    LongArray(this.size) { i -> transform(this[i]) }

/**
 * Maps a [List] to a [FloatArray].
 */
inline fun <reified T> List<T>.mapToFloatArray(transform: (T) -> Float): FloatArray =
    FloatArray(this.size) { i -> transform(this[i]) }

/**
 * Returns a randomly generated [Color].
 */
fun kotlin.random.Random.nextColor(
    hue: Float = 1f,
    saturation: Float = 0.7f,
    luminance: Float = 0.9f,
): Color {
    val c = java.awt.Color.getHSBColor(
        hue * nextFloat(),
        saturation + (1f - saturation) * nextFloat(),
        luminance + (1f - luminance) * nextFloat()
    )
    return Color(c.red / 255f, c.green / 255f, c.blue / 255f)
}

/**
 * Returns the value after the last '_' as a float, or null.
 */
fun String.getEventValue() = this.substringAfterLast("_").toFloatOrNull()