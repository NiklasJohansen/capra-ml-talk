package presentation

import com.fasterxml.jackson.annotation.JsonIgnore
import data.Dataset
import network.Network
import network.Node
import network.Trainer
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.surface.Surface
import no.njoh.pulseengine.core.shared.primitives.Color
import tools.format
import tools.setDrawColor
import kotlin.math.max

/**
 * Text label for displaying the Mean Squared Error (MSE) of a network.
 */
class ErrorLabel : PresentationEntity()
{
    /** ID of the [Dataset] to measure the error from. Optional if the [trainerId] is set. */
    var datasetId = -1L

    /** ID of the [Trainer] to source error calculations from. Optional if error is fetched from a [Dataset]. */
    var trainerId = -1L

    /** Set true to square the final error calculation. */
    var squareError = true

    /** Set true of the error should be the sum of all samples in the dataset or just the current sample. */
    var sumAllSamples = false

    /** Of error should be calculated for only a specific sample in the [Dataset]. */
    var useOnlySampleIndex = -1

    // Styling parameters
    var textSize = 72f
    var fontName = ""
    var color = Color(1f, 1f, 1f)
    var xOrigin = 0.5f

    @JsonIgnore var network = Network.EMPTY

    override fun onStart(engine: PulseEngine)
    {
        super.onStart(engine)
        this.network = Network.generateFromDatasetId(engine, datasetId)
    }

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface)
    {
        val meanSquaredError = getErrorFromTrainer(engine) ?: calculateErrorFromNetwork(engine) ?: return
        surface.setDrawColor(color, visibility)
        surface.drawText(
            text = meanSquaredError.format(),
            x = x,
            y = y,
            font = engine.asset.getOrNull(fontName),
            fontSize = textSize,
            angle = rotation,
            xOrigin = xOrigin,
            yOrigin = 0.5f
        )
    }

    private fun calculateErrorFromNetwork(engine: PulseEngine): Float?
    {
        val dataset = engine.scene.getEntityOfType<Dataset>(datasetId) ?: return null
        val maxSampleCount = if (sumAllSamples) dataset.getSampleCount() else 1
        var errorSum = 0f
        var samples = 0

        for (sample in 0 until maxSampleCount)
        {
            if (useOnlySampleIndex < 0 || useOnlySampleIndex == dataset.selectedSampleIndex)
            {
                // Sum up errors for all output nodes
                for (id in network.getOutputNodeIds())
                {
                    val node = engine.scene.getEntityOfType<Node>(id) ?: continue
                    val targetValue = dataset.getAttributeValue(node.targetValueIndex)
                    var error = node.outputValue - targetValue
                    if (squareError)
                        error *= error
                    errorSum += error
                    samples++
                }
            }

            // Move to next sample and compute output values
            if (maxSampleCount > 1)
            {
                dataset.selectNextSample()
                network.compute(engine)
            }
        }

        // Calculate mean squared error (MSE)
        return errorSum / max(samples, 1)
    }

    private fun getErrorFromTrainer(engine: PulseEngine): Float?
    {
        val trainer = engine.scene.getEntityOfType<Trainer>(trainerId) ?: return null
        return if (trainer.trainedBatches > 0) trainer.meanSquaredError else null
    }

    override fun onEventMessage(engine: PulseEngine, eventMessage: String)
    {
        when (eventMessage)
        {
            "SQUARE_ERROR_ON" -> squareError = true
            "SQUARE_ERROR_OFF" -> squareError = false
        }
    }
}