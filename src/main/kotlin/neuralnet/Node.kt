package neuralnet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color
import presentation.StyleSystem
import kotlin.math.min
import tools.editEntityValue
import tools.format
import tools.lerp
import kotlin.math.abs

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

    /** If the [Node] is an output node this index defines which column in the [Dataset] is the ideal target value. */
    @JsonAlias("targetValueIndex")
    var idealValueIndex = -1

    // Styling and interaction parameters
    var textColor = Color(255, 255, 255)
    var textSize = 28f
    var showText = true
    var editable = true
    var showActivationFunction = true
    var borderSize = 3.5f

    // Store weighted sum (sum before activation function) for use under training
    @JsonIgnore var weightedSum = 0f

    override fun onUpdate(engine: PulseEngine)
    {
        updateNodeValue(engine)
    }

    /**
     * Updates this nodes output value either by looking it up in a [Dataset] or by calculating
     * it from other connected [Node]s.
     */
    fun updateNodeValue(engine: PulseEngine)
    {
        // Get node value from dataset if datasetId references a Dataset entity and the idealValueIndex is
        // not set (indicating it is an output node)
        if (datasetId >= 0 && attributeIndex >= 0 && idealValueIndex < 0)
        {
            engine.scene.getEntityOfType<Dataset>(datasetId)?.let()
            {
                outputValue = it.getSelectedValueAsFloat(attributeIndex)
                return // No need to calculate further when source is dataset
            }
        }

        // Calculate the output value from connected nodes
        weightedSum = 0f
        var updateValue = false
        engine.scene.forEachEntityOfType<Connection> { connection ->
            if (connection.toNodeId == this.id)
            {
                engine.scene.getEntityOfType<Node>(connection.fromNodeId)?.let { fromNode ->
                    weightedSum += fromNode.outputValue * connection.weight
                    updateValue = true
                }
            }
        }

        if (updateValue) // Got updated value from connected nodes
        {
            outputValue = activationFunction.compute(weightedSum)
        }
        else if (editable) // Manually edit value
        {
            outputValue += editEntityValue(engine, id)
        }
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        // Get colors from StyleSystem
        val style = engine.scene.getSystemOfType<StyleSystem>()
        val lowColor = style?.nodeLowColor ?: lowFillColor
        val highColor = style?.nodeHighColor ?: highFillColor
        val borderColor = style?.nodeBorderColor ?: borderColor
        val a = abs(outputValue).coerceAtMost(1f)

        // Border
        surface.setDrawColor(borderColor)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = min(width, height) * 0.5f)

        // Fill
        surface.setDrawColor(
            red = lerp(lowColor.red, highColor.red, a),
            green = lerp(lowColor.green, highColor.green, a),
            blue = lerp(lowColor.blue, highColor.blue, a)
        )
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
        if (showText)
        {
            surface.setDrawColor(textColor)
            surface.drawText(outputValue.format(), x, y, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = textSize)
        }

        if (!showActivationFunction || activationFunction == ActivationFunction.NONE)
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

    companion object
    {
        private var lowFillColor = Color(47, 68, 94)
        private var highFillColor = Color(94, 136, 188)
        private var borderColor = Color(7, 7, 7)
    }
}