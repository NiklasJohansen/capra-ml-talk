package neuralnet
import kotlin.math.exp

/**
 * This enum class holds the available activation functions and the lambdas to calculate them.
 */
enum class ActivationFunction(
    val compute: (Float) -> Float,
    val computeDerivative: (Float) -> Float
) {
    NONE(
        compute = { it },
        computeDerivative = { it }
    ),

    SIGMOID(
        compute = { sigmoid(it) },
        computeDerivative = { sigmoid(it) * (1f - sigmoid(it)) }
    );

    companion object
    {
        fun sigmoid(value: Float) = 1.0f / (1.0f + exp(-value))
    }
}