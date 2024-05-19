package presentation

import com.fasterxml.jackson.annotation.JsonIgnore
import network.Node
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Font
import no.njoh.pulseengine.core.graphics.surface.Surface
import no.njoh.pulseengine.core.shared.primitives.Color
import tools.mapToLongArray
import tools.setDrawColor
import kotlin.math.min

/**
 * Graphical classifier entity for showing text labels based on the output of a network.
 */
class Classifier : PresentationEntity()
{
    /** IDs of the output [Node]s to source values from. */
    var outputNodeIds = ""

    /** The text labels associated with each class. */
    var classLabels = ""

    // Styling
    var textColor = Color(0f, 0f, 0f)
    var textSize = 24f
    var fontName = ""

    @JsonIgnore private var nodeIds = LongArray(0)
    @JsonIgnore private var labels = Array(0) { "" }

    override fun onStart(engine: PulseEngine)
    {
        super.onStart(engine)
        nodeIds = outputNodeIds.split(",").mapToLongArray { it.toLongOrNull() ?: -1 }
        labels = classLabels.split(",").toTypedArray()
    }

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface)
    {
        // Calculate total sum
        var sum = 0.0000001f
        for (id in nodeIds)
        {
            val outputValue = engine.scene.getEntityOfType<Node>(id)?.outputValue ?: continue
            sum += outputValue * outputValue
        }

        val font = engine.asset.getOrNull<Font>(fontName)
        for (i in 0 until min(nodeIds.size, labels.size))
        {
            // Calculate alpha for each class based on the amount it contributes to the total sum
            val outputValue = engine.scene.getEntityOfType<Node>(nodeIds[i])?.outputValue ?: continue
            val alpha = ((outputValue * outputValue) / sum).coerceIn(0f, 1f)

            // Draw each text label
            surface.setDrawColor(textColor, visibility * alpha)
            surface.drawText(
                text = labels[i],
                x = x,
                y = y,
                font = font,
                fontSize = textSize,
                angle = rotation,
                xOrigin = 0.5f,
                yOrigin = 0.5f
            )
        }
    }
}