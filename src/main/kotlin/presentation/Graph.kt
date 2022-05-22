package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color
import tools.format

/**
 * This entity is used to visualize data points over time.
 * All entities that implement the [Graphable] interface can provide data to this graph entity.
 */
class Graph : SceneEntity()
{
    /** The ID of the entity to graph. */
    var entityIdToGraph = -1L

    /** The highest X value of the graph. */
    var xMaxValue = 300f

    /** The highest Y value of the graph. */
    var yMaxValue = 0.5f

    // Styling parameters
    var backgroundColor = Color(245, 245, 245)
    var axisColor = Color(0, 0, 0)
    var lineColor = Color(255, 0, 0)
    var tickTextColor = Color(0, 0, 0)
    var valueTextColor = Color(255, 0, 0)
    var tickTextSize = 15f
    var valueTextSize = 15f
    var axisThickness = 5f
    var tickThickness = 1f
    var tickLength = 4f
    var yAxisTickCount = 5
    var xAxisTickCount = 5
    var xAxisShowDecimal = true
    var yAxisShowDecimal = true

    /**
     * Render the graph.
     */
    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val xOrigin = x - width * 0.5f
        val yOrigin = y + height * 0.5f

        // Draw background rectangle
        surface.setDrawColor(backgroundColor)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f)

        // Draw Y-axis
        surface.setDrawColor(axisColor)
        surface.drawTexture(Texture.BLANK, xOrigin - axisThickness * 0.5f, yOrigin, axisThickness, -height)
        for (i in 0 until yAxisTickCount + 1)
        {
            val yPos = yOrigin - (i.toFloat() / yAxisTickCount) * height - tickThickness * 0.5f
            surface.setDrawColor(axisColor)
            surface.drawTexture(
                texture = Texture.BLANK,
                x = xOrigin - tickLength * 0.5f - axisThickness * 0.5f,
                y = yPos,
                width = tickLength + axisThickness,
                height = tickThickness
            )

            val text = getTickText((i.toFloat() / yAxisTickCount) * yMaxValue, isXAxis = false)
            surface.setDrawColor(tickTextColor)
            surface.drawText(text, xOrigin - tickTextSize * 0.6f, yPos, xOrigin = 1f, yOrigin = 0.5f, fontSize = tickTextSize)
        }

        // Draw X-axis
        surface.setDrawColor(axisColor)
        surface.drawTexture(Texture.BLANK, xOrigin, yOrigin - axisThickness * 0.5f, width, axisThickness)
        for (i in 0 until xAxisTickCount + 1)
        {
            val xPos = xOrigin + (i.toFloat() / xAxisTickCount) * width - tickThickness * 0.5f
            surface.setDrawColor(axisColor)
            surface.drawTexture(
                texture = Texture.BLANK,
                x = xPos,
                y = yOrigin - tickLength * 0.5f - axisThickness * 0.5f,
                width = tickThickness,
                height = tickLength + axisThickness
            )

            val text = getTickText((i.toFloat() / xAxisTickCount) * xMaxValue, isXAxis = true)
            surface.setDrawColor(tickTextColor)
            surface.drawText(text, xPos, yOrigin + tickTextSize, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = tickTextSize)
        }

        val values = getValueList(engine)
        if (values == null || values.isEmpty())
            return // No values to graph

        // Draw Graph line
        surface.setDrawColor(lineColor)
        for (i in 1 until values.size)
        {
            val x0 = xOrigin + width * (values[i - 1].first.toFloat() / xMaxValue)
            val y0 = yOrigin - height * (values[i - 1].second.toFloat() / yMaxValue)
            val x1 = xOrigin + width * (values[i].first.toFloat() / xMaxValue)
            val y1 = yOrigin - height * (values[i].second.toFloat() / yMaxValue)
            surface.drawLine(x0, y0, x1, y1)
        }

        val lastValue = values.last()
        val x0 = xOrigin + width * (lastValue.first.toFloat() / xMaxValue) + 5
        val y0 = yOrigin - height * (lastValue.second.toFloat() / yMaxValue) - 5
        val text = lastValue.second.toString()
        surface.setDrawColor(valueTextColor)
        surface.drawText(text, x0, y0, xOrigin = 0f, yOrigin = 0.5f, fontSize = valueTextSize)
    }

    /**
     * Returns the value list of the [Graphable] entity.
     */
    private fun getValueList(engine: PulseEngine): MutableList<out Pair<Number, Number>>?
    {
        val entity = engine.scene.getEntity(entityIdToGraph)
        return if (entity is Graphable<*, *>) entity.graphValues else null
    }

    /**
     * Converts the [value] to a string with or without decimals.
     */
    private fun getTickText(value: Float, isXAxis: Boolean): String = when
    {
        isXAxis -> if (xAxisShowDecimal) value.format() else value.toInt().toString()
        else    -> if (yAxisShowDecimal) value.format() else value.toInt().toString()
    }
}

/**
 * Interface for entities to provide values that can be graphed by the [Graph] class.
 */
interface Graphable <X : Number, Y : Number>
{
    /** Values added to this list will be graphed. */
    val graphValues: MutableList<Pair<X, Y>>
}