package presentation

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.scene.SceneSystem
import kotlin.math.max
import kotlin.math.min

/**
 * Animation system used to capture and animate float values over time.
 */
class Animator : SceneSystem()
{
    /** The default duration of an animation in milliseconds. */
    var defaultAnimationTimeMs = 200L

    @JsonIgnore
    private val activeAnimations = mutableListOf<Animation>()

    override fun onUpdate(engine: PulseEngine)
    {
        val timeSinceLastUpdateMs = engine.data.deltaTime * 1000f
        for (animation in activeAnimations)
        {
            val changeRate = timeSinceLastUpdateMs / animation.overTimeMs.coerceAtLeast(1L)
            val minValue = min(animation.fromValue, animation.toValue)
            val maxValue = max(animation.fromValue, animation.toValue)
            val newValue = animation.currentValue + changeRate * (animation.toValue - animation.fromValue)

            animation.currentValue = newValue.coerceIn(minValue, maxValue)
            animation.setTarget(animation.currentValue)
        }

        activeAnimations.removeIf { it.isFinished() }
    }

    /**
     * Adds a target float to be animated.
     * @param from the value the animation will start from
     * @param to the target value the animation will end on
     * @param overTimeMs the duration of the animation in milliseconds
     * @param setTarget the target lambda function to set the value to be animated
     */
    fun addAnimationTarget(from: Float, to: Float, overTimeMs: Long? = null, setTarget: (value: Float) -> Unit)
    {
        activeAnimations.add(Animation(from, to, overTimeMs ?: defaultAnimationTimeMs, setTarget))
    }

    private data class Animation(
        val fromValue: Float,
        val toValue: Float,
        val overTimeMs: Long,
        val setTarget: (Float) -> Unit,
        var currentValue: Float = fromValue,
        var finished: Boolean = false
    ) {
        fun isFinished() = (currentValue == toValue)
    }
}