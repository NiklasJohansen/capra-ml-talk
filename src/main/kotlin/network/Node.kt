package network

import com.fasterxml.jackson.annotation.JsonIgnore
import data.DataSource
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.primitives.Color
import presentation.PresentationEntity
import presentation.StyleSystem
import kotlin.math.min
import tools.editEntityValue
import tools.format
import tools.lerp
import tools.setDrawColor
import kotlin.math.abs

/**
 * This entity represent a neuron / brain-cell.
 */
class Node : PresentationEntity()
{
    /** The output value of the node. */
    var outputValue = 0.5f

    /** The activation function to apply when calculating the outputValue from the connected input nodes. */
    var activationFunction = ActivationFunction.SIGMOID

    /** The ID of the [DataSource] to source the output value from. Set to -1 if no data source should be used. */
    var dataSourceId = -1L

    /** The column index of the [DataSource] to sample the input attribute from. Used when the [Node] is an input node. */
    var attributeValueIndex = -1

    /** If the [Node] is an output node this index defines the column containing the target value in the [DataSource]. */
    var targetValueIndex = -1

    // Styling and interaction parameters
    var textColor = Color(255, 255, 255)
    var textSize = 28f
    var showText = true
    var editable = true
    var showActivationFunction = true
    var borderSize = 3.5f

    /** Sum of all incoming connections before activation function is applied. */
    @JsonIgnore var weightedSum = 0f

    /** Local cached array of IDs for incoming [Node]s and [Connection]s. */
    @JsonIgnore private var incomingConnectionIds = LongArray(0)

    override fun onStart(engine: PulseEngine)
    {
        super.onStart(engine)

        // Create a local array of the IDs to all incoming connection (for faster lookup while computing outputValue)
        incomingConnectionIds = engine.scene.getAllEntitiesOfType<Connection>()
            ?.mapNotNull { con -> if (con.toNodeId == this.id) con.id else null }
            ?.toLongArray()
            ?: incomingConnectionIds
    }

    override fun onUpdate(engine: PulseEngine)
    {
        if (updateNodeValues)
            computeOutputValue(engine)
    }

    /**
     * Updates the outputValue of the [Node].
     * Will source the value from the specified [DataSource] or compute the value from incoming [Connection]s.
     */
    fun computeOutputValue(engine: PulseEngine)
    {
        // Try to source the output value from the referenced data source
        if (dataSourceId >= 0 && attributeValueIndex >= 0 && targetValueIndex < 0)
        {
            engine.scene.getEntityOfType<DataSource>(dataSourceId)?.let()
            {
                outputValue = it.getAttributeValue(attributeValueIndex)
                return // No need to compute further when source is data source
            }
        }

        // Reset previously calculated weightedSum
        val lastWeightedSum = weightedSum
        weightedSum = 0f

        // Compute weightedSum value from connected nodes
        for (connectionId in incomingConnectionIds)
        {
            val connection = engine.scene.getEntityOfType<Connection>(connectionId) ?: continue
            val fromNode = engine.scene.getEntityOfType<Node>(connection.fromNodeId) ?: continue
            weightedSum += fromNode.outputValue * connection.weight
        }

        if (weightedSum != lastWeightedSum) // Compute outputValue if weightedSum changed
        {
            outputValue = activationFunction.compute(weightedSum)
        }
        else if (editable) // Edit value by mouse input if enabled
        {
            outputValue += editEntityValue(engine, id)
        }
    }

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        // Get colors from StyleSystem
        val style = engine.scene.getSystemOfType<StyleSystem>()
        val lowColor = style?.nodeLowColor ?: lowFillColor
        val highColor = style?.nodeHighColor ?: highFillColor
        val borderColor = style?.nodeBorderColor ?: borderColor
        val a = abs(outputValue).coerceAtMost(1f)

        // Border
        surface.setDrawColor(borderColor, visibility)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = min(width, height) * 0.5f)

        // Fill
        surface.setDrawColor(
            red = lerp(lowColor.red, highColor.red, a),
            green = lerp(lowColor.green, highColor.green, a),
            blue = lerp(lowColor.blue, highColor.blue, a),
            alpha = visibility
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
            surface.setDrawColor(textColor, visibility)
            surface.drawText(outputValue.format(), x, y, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = textSize)
        }

        if (!showActivationFunction || activationFunction == ActivationFunction.NONE)
            return

        // Activation indicator border
        var size = 12f
        surface.setDrawColor(borderColor, visibility)
        surface.drawTexture(Texture.BLANK, x - width * 0.5f, y, size, size, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = size * 0.5f)

        // Activation indicator fill
        size -= borderSize * 0.6f
        surface.setDrawColor(1f, 0.75f, 0f, visibility)
        surface.drawTexture(Texture.BLANK, x - width * 0.5f, y, size, size, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = size * 0.5f)

        // Activation indicator text
        surface.setDrawColor(borderColor, visibility)
        surface.drawText(activationFunction.name.take(1), x - width * 0.5f, y + size * 0.1f, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = size)
    }

    /**
     * Returns true if the xPos,yPos coordinate is inside the radius of the [Node]
     */
    fun isInside(xPos: Float, yPos: Float): Boolean
    {
        val radius = min(width, height) * 0.5f
        return (xPos - x) * (xPos - x) + (yPos - y) * (yPos - y) < radius * radius
    }

    companion object
    {
        /** Set true if nodes should be responsible for updating their own output values. */
        var updateNodeValues = true

        // Default color values
        private var lowFillColor = Color(47, 68, 94)
        private var highFillColor = Color(94, 136, 188)
        private var borderColor = Color(7, 7, 7)
    }
}