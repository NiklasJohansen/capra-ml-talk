package network
import kotlin.math.exp
import kotlin.math.tanh

/**
 * This enum class holds the available activation functions and the lambdas to calculate them.
 */
enum class ActivationFunction(
    val compute: (Float) -> Float,
    val computeDerivative: (Float) -> Float
) {
    /** Applies no transformation to the value. */
    NONE(
        compute = { it },
        computeDerivative = { it }
    ),

    /** The Sigmoid activation function normalizes the input to a value between 0.0 and 1.0. */
    SIGMOID(
        compute = { 1.0f / (1.0f + exp(-it)) },
        computeDerivative = {
            val v = 1.0f / (1.0f + exp(-it))
            v * (1f - v)
        }
    ),

    /** The Hyperbolic Tangent function normalizes the input to value between -1.0 and 1.0. */
    TANH(
        compute = { tanh(it) },
        computeDerivative = {
            val v = tanh(it)
            1f - v * v
        }
    );
}