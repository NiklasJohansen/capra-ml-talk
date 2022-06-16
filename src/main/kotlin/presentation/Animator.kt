package presentation

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.scene.SceneSystem
import java.util.LinkedList
import kotlin.math.*
import kotlin.reflect.KMutableProperty

/**
 * Animation system used to animate properties over time.
 */
class Animator : SceneSystem()
{
    /** The default duration of an animation in milliseconds. */
    var defaultAnimationTimeMs = 200L

    /** The default easing function to use when animating the value. */
    var defaultEasingFunction = EasingFunction.EASE_IN_OUT_CUBIC

    @JsonIgnore
    private val activeAnimations = LinkedList<Animation>()

    /**
     * Handle all active animations.
     */
    override fun onUpdate(engine: PulseEngine)
    {
        val currentTime = System.nanoTime()
        activeAnimations.removeIf { animation ->
            // Calculate current value of property based on elapsed time and easing function
            val elapsedTimeNano = currentTime - animation.startTimeNano
            val t = (elapsedTimeNano / animation.durationNano).coerceIn(0.0, 1.0)
            val delta = animation.toValue - animation.fromValue
            val currentValue = animation.fromValue + delta * animation.easingFunction.compute(t)

            // Update value of property
            animation.property.setter.call(currentValue.toFloat())

            // Remove animation instance if it is finished
            elapsedTimeNano >= animation.durationNano
        }
    }

    /**
     * Adds a property to be animated.
     * @param property the property to animated
     * @param targetValue the target value the animation will end on
     * @param durationMs the duration of the animation in milliseconds
     * @param easingFunction the function to use when animating the property to the target value
     */
    fun addAnimation(
        property: KMutableProperty<Float>,
        targetValue: Float,
        durationMs: Long? = null,
        easingFunction: EasingFunction? = null
    ) {
        activeAnimations.add(Animation(
            property = property,
            fromValue = property.getter.call(),
            toValue = targetValue,
            durationNano = (durationMs ?: defaultAnimationTimeMs) * 1_000_000.0,
            easingFunction = easingFunction ?: defaultEasingFunction,
        ))
    }

    private data class Animation(
        val property: KMutableProperty<Float>,
        val fromValue: Float,
        val toValue: Float,
        val durationNano: Double,
        val easingFunction: EasingFunction,
        val startTimeNano: Long = System.nanoTime(),
        var isFinished: Boolean = false
    )

    /**
     * Defines easing functions specifying the rate of change of the animation over time.
     * Sourced from: https://easings.net/
     */
    enum class EasingFunction
    {
        LINEAR,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC;

        fun compute(t: Double) = when (this)
        {
            LINEAR -> t
            EASE_IN_CUBIC -> t * t * t
            EASE_OUT_CUBIC -> 1.0 - (1.0 - t).pow(3.0)
            EASE_IN_OUT_CUBIC -> if (t < 0.5) 4.0 * t * t * t else 1.0 - 0.5 * (-2.0 * t + 2.0).pow(3)
        }
    }
}