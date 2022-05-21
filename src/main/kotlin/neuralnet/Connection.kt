package neuralnet

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color
import tools.editEntityValue
import tools.format
import tools.times
import kotlin.math.abs
import kotlin.math.min

/**
 * This entity represent a connection between two [Node]s.
 */
class Connection : SceneEntity()
{
    /** The weight value of this connection. */
    var weight = 0f

    /** The ID of the [Node] the connection comes from. */
    var fromNodeId = -1L

    /** The ID of the [Node] the connection is going into. */
    var toNodeId = -1L

    // Styling parameters of the connection
    var negativeColor = Color(1f, 0f, 0f)
    var positiveColor = Color(0f, 1f, 0f)
    var textColor = Color(7, 7, 7)
    var showText = true
    var editable = true
    var textSize = 15f
    var xTextOffset = 0f
    var yTextOffset = 0f
    var lineThickness = 4f

    override fun onUpdate(engine: PulseEngine)
    {
        if (editable)
            weight += editEntityValue(engine, id)
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val fromNode = engine.scene.getEntityOfType<Node>(fromNodeId)
        val toNode = engine.scene.getEntityOfType<Node>(toNodeId)

        if (fromNode != null && toNode != null)
            drawConnection(surface, fromNode, toNode)
    }

    private fun drawConnection(surface: Surface2D, fromNode: Node, toNode: Node)
    {
        // Draw line
        val halfLineWidth = getLineWidth(weight, lineThickness) * 0.5f
        surface.setDrawColor(getLineColor(weight))
        surface.drawQuadVertex(fromNode.x + fromNode.width * 0.5f, fromNode.y - halfLineWidth)
        surface.drawQuadVertex(fromNode.x + fromNode.width * 0.5f, fromNode.y + halfLineWidth)
        surface.drawQuadVertex(toNode.x - toNode.width * 0.5f, toNode.y + halfLineWidth)
        surface.drawQuadVertex(toNode.x - toNode.width * 0.5f, toNode.y - halfLineWidth)

        // Draw weight value text if enabled
        if (showText)
        {
            x = (fromNode.x + toNode.x) * 0.5f + xTextOffset
            y = (fromNode.y + toNode.y) * 0.5f + yTextOffset
            surface.setDrawColor(textColor)
            surface.drawText(
                text = weight.format(),
                x = x,
                y = y,
                xOrigin = 0.5f,
                yOrigin = 0.5f,
                fontSize = textSize
            )
        }
    }

    private fun getLineColor(weight: Float): Color = when
    {
        weight < 0f -> negativeColor * min(-weight, 1f)
        weight > 0f -> positiveColor * min(weight, 1f)
        else -> Color(0f, 0f, 0f)
    }

    private fun getLineWidth(weight: Float, maxLineWidth: Float): Float =
        abs(weight).coerceIn(0.3f, 1f) * maxLineWidth

    companion object
    {
        const val NOT_SET = -1L
    }
}