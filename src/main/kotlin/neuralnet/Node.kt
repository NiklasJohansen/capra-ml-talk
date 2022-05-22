package neuralnet

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color
import kotlin.math.min
import tools.editEntityValue
import tools.format
import tools.times

/**
 * This entity represent a neuron / brain-cell.
 */
class Node : SceneEntity()
{
    /** The output value of the node. */
    var outputValue = 0.5f

    /** The activation function to apply when calculating the outputValue from the connected input nodes. */
    var activationFunction = ActivationFunction.SIGMOID

    /** The ID of the [Dataset] to source the output value from. Set to -1 if no dataset should be used. */
    var datasetId = -1L

    /** The column index of the [Dataset] to sample the input attribute from. Used when the [Node] is an input node. */
    var attributeIndex = -1

    /** If the [Node] is an output node this index defines which column in the [Dataset] is the actual target value. */
    var targetValueIndex = -1

    // Styling and interaction parameters
    var fillColor = Color(47, 68, 94)
    var borderColor = Color(7, 7, 7)
    var textColor = Color(255, 255, 255)
    var textSize = 28f
    var borderSize = 3.5f
    var editable = true

    // Store weighted sum (value before activation function) for use under training
    @JsonIgnore var weightedSum = 0f

    override fun onUpdate(engine: PulseEngine)
    {
        if (editable)
            outputValue += editEntityValue(engine, id)

        updateNodeValue(engine)
    }

    /**
     * Updates this nodes output value either by looking it up in a [Dataset] or by calculating
     * it from other connected [Node]s.
     */
    fun updateNodeValue(engine: PulseEngine)
    {
        // Get node value from dataset if datasetId references a Dataset entity and the targetValueIndex is
        // not set (indicating it is an output node)
        if (datasetId >= 0 && attributeIndex >= 0 && targetValueIndex < 0)
        {
            engine.scene.getEntityOfType<Dataset>(datasetId)?.let()
            {
                outputValue = it.getSelectedValueAsFloat(attributeIndex)
                return // No need to calculate further when source is dataset
            }
        }

        // Calculate the output value from connected Nodes.
        var sum = 0f
        var updateValue = false
        engine.scene.forEachEntityOfType<Connection> { con ->
            if (con.toNodeId == this.id)
            {
                engine.scene.getEntityOfType<Node>(con.fromNodeId)?.let { fromNode ->
                    sum += fromNode.outputValue * con.weight
                    updateValue = true
                }
            }
        }

        if (updateValue)
        {
            weightedSum = sum
            outputValue = activationFunction.compute(sum)
        }
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        // Border
        surface.setDrawColor(borderColor)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = min(width, height) * 0.5f)

        // Fill
        val fillColor = if (editable && isInside(engine.input.xWorldMouse, engine.input.yWorldMouse)) fillColor * 1.1f else fillColor
        surface.setDrawColor(fillColor)
        surface.drawTexture(
            texture = Texture.BLANK,
            x = x,
            y = y,
            width = width - borderSize,
            height = height - borderSize,
            xOrigin = 0.5f,
            yOrigin = 0.5f,
            cornerRadius = (min(width, height) - borderSize) * 0.5f
        )

        // Text
        surface.setDrawColor(textColor)
        surface.drawText(outputValue.format(), x, y, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = textSize)

        if (activationFunction == ActivationFunction.NONE)
            return

        // Activation indicator border
        var size = 12f
        surface.setDrawColor(borderColor)
        surface.drawTexture(Texture.BLANK, x - width * 0.5f, y, size, size, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = size * 0.5f)

        // Activation indicator fill
        size -= borderSize * 0.6f
        surface.setDrawColor(1f, 0.75f, 0f)
        surface.drawTexture(Texture.BLANK, x - width * 0.5f, y, size, size, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = size * 0.5f)

        // Activation indicator text
        surface.setDrawColor(borderColor)
        surface.drawText(activationFunction.name.take(1), x - width * 0.5f, y + size * 0.1f, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = size)
    }

    fun isInside(xPos: Float, yPos: Float): Boolean
    {
        val radius = min(width, height) * 0.5f
        return (xPos - x) * (xPos - x) + (yPos - y) * (yPos - y) < radius * radius
    }
}