package network

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.primitives.Color
import presentation.PresentationEntity
import presentation.StyleSystem
import tools.editEntityValue
import tools.format
import tools.setDrawColor
import kotlin.math.abs
import kotlin.math.max

/**
 * This entity represent a connection between two [Node]s.
 */
class Connection : PresentationEntity()
{
    /** The weight value of this connection. */
    var weight = 0f

    /** The ID of the [Node] the connection comes from. */
    var fromNodeId = -1L

    /** The ID of the [Node] the connection is going into. */
    var toNodeId = -1L

    // Styling parameters of the connection
    var textColor = Color(7, 7, 7)
    var textSize = 15f
    var showText = true
    var editable = true
    var lineThickness = 4f

    override fun onUpdate(engine: PulseEngine)
    {
        if (editable)
            weight += editEntityValue(engine, id)

        // Removes disconnected connections
        if (engine.scene.getEntity(fromNodeId) == null || engine.scene.getEntity(toNodeId) == null)
            set(DEAD)
    }

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        val fromNode = engine.scene.getEntityOfType<Node>(fromNodeId)
        val toNode = engine.scene.getEntityOfType<Node>(toNodeId)

        if (fromNode != null && toNode != null)
            drawConnection(engine, surface, fromNode, toNode)
    }

    private fun drawConnection(engine: PulseEngine, surface: Surface2D, fromNode: Node, toNode: Node)
    {
        // Draw line
        val halfLineWidth = getLineWidth(weight, lineThickness) * 0.5f
        surface.setLineColor(engine, weight)
        surface.drawQuadVertex(fromNode.x + fromNode.width * 0.5f, fromNode.y - halfLineWidth)
        surface.drawQuadVertex(fromNode.x + fromNode.width * 0.5f, fromNode.y + halfLineWidth)
        surface.drawQuadVertex(toNode.x - toNode.width * 0.5f, toNode.y + halfLineWidth)
        surface.drawQuadVertex(toNode.x - toNode.width * 0.5f, toNode.y - halfLineWidth)

        // Draw weight value text if enabled
        if (showText)
        {
            surface.setDrawColor(textColor, visibility)
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

    private fun Surface2D.setLineColor(engine: PulseEngine, weight: Float)
    {
        val style = engine.scene.getSystemOfType<StyleSystem>()
        val negativeColor = style?.connectionNegativeColor ?: negativeColor
        val positiveColor = style?.connectionPositiveColor ?: positiveColor
        val w = weight.coerceIn(-1f, 1f)
        val alpha = max(abs(w), 0.5f) * visibility
        when
        {
            weight < 0 -> setDrawColor(negativeColor.red, negativeColor.green, negativeColor.blue, alpha)
            else -> setDrawColor(positiveColor.red, positiveColor.green, positiveColor.blue, alpha)
        }
    }

    private fun getLineWidth(weight: Float, maxLineWidth: Float): Float =
        abs(weight).coerceIn(0.2f, 1f) * maxLineWidth

    companion object
    {
        const val NOT_SET = -1L
        private var negativeColor = Color(1f, 0f, 0f)
        private var positiveColor = Color(0f, 1f, 0f)
    }
}