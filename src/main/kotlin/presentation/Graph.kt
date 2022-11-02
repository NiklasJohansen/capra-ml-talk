package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.forEachFast
import no.njoh.pulseengine.core.shared.utils.Extensions.toDegrees
import no.njoh.pulseengine.core.shared.utils.MathUtil
import tools.format
import tools.setDrawColor
import kotlin.math.sqrt

/**
 * This entity is used to visualize data points over time.
 * All entities that implement the [Graphable] interface can provide data to this graph entity.
 */
class Graph : PresentationEntity()
{
    /** The ID of the entity to graph. */
    var entityIdToGraph = -1L

    /** The ID of the entity to plot. */
    var entityIdToPlot = -1L

    /** The lowest X value of the graph. */
    var xMinValue = 0f

    /** The highest X value of the graph. */
    var xMaxValue = 300f

    /** The lowest Y value of the graph. */
    var yMinValue = 0.0f

    /** The highest Y value of the graph. */
    var yMaxValue = 0.5f

    // Styling parameters
    var backgroundColor = Color(245, 245, 245)
    var axisColor = Color(0, 0, 0)
    var lineColor = Color(255, 0, 0)
    var tickTextColor = Color(0, 0, 0)
    var plotPointColor = Color(0, 0, 0)
    var plotPointTextColor = Color(255, 0, 0)
    var plotPointTextsize = 15f
    var plotPointSize = 30f
    var lineThickness = 1f
    var tickTextSize = 15f
    var axisThickness = 5f
    var tickThickness = 1f
    var tickLength = 4f
    var yAxisTickCount = 5
    var xAxisTickCount = 5
    var xSkipOriginTick = false
    var ySkipOriginTick = false
    var xAxisShowDecimal = true
    var yAxisShowDecimal = true

    /**
     * Render the graph.
     */
    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        // Draw background rectangle
        surface.setDrawColor(backgroundColor, visibility)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f)

        // Draw graph lines
        drawGraphLines(surface, engine)

        // Draw graph axes with tick marks
        drawGraphAxis(surface)

        // Draw plot points
        drawPlotPoints(surface, engine)
    }

    private fun drawGraphAxis(surface: Surface2D)
    {
        val xRange = xMaxValue - xMinValue
        val yRange = yMaxValue - yMinValue
        val xLeft = x - width * 0.5f
        val yTop = y - height * 0.5f
        val xOrigin = xLeft + width * (1f - xMaxValue / xRange).coerceIn(0f, 1f)
        val yOrigin = yTop + height * (yMaxValue / yRange).coerceIn(0f, 1f)

        // Draw Y-axis
        surface.setDrawColor(axisColor, visibility)
        surface.drawTexture(Texture.BLANK, xOrigin - axisThickness * 0.5f, yTop, axisThickness, height)
        for (i in 0 until yAxisTickCount + 1)
        {
            val yPos = yTop + (i.toFloat() / yAxisTickCount) * height - tickThickness * 0.5f
            if (ySkipOriginTick && yPos < yOrigin && yPos + height / yAxisTickCount >= yOrigin )
                continue

            surface.setDrawColor(axisColor, visibility)
            surface.drawTexture(
                texture = Texture.BLANK,
                x = xOrigin - tickLength * 0.5f - axisThickness * 0.5f,
                y = yPos,
                width = tickLength + axisThickness,
                height = tickThickness
            )

            val text = getTickText(yMaxValue - yRange * (i.toFloat() / yAxisTickCount), isXAxis = false)
            surface.setDrawColor(tickTextColor, visibility)
            surface.drawText(text, xOrigin - tickTextSize * 0.5f, yPos, xOrigin = 1f, yOrigin = 0.5f, fontSize = tickTextSize)
        }

        // Draw X-axis
        surface.setDrawColor(axisColor, visibility)
        surface.drawTexture(Texture.BLANK, xLeft, yOrigin - axisThickness * 0.5f, width, axisThickness)
        for (i in 0 until xAxisTickCount + 1)
        {
            val xPos = xLeft + (i.toFloat() / xAxisTickCount) * width - tickThickness * 0.5f
            if (xSkipOriginTick && xPos < xOrigin && xPos + width / xAxisTickCount >= xOrigin )
                continue

            surface.setDrawColor(axisColor, visibility)
            surface.drawTexture(
                texture = Texture.BLANK,
                x = xPos,
                y = yOrigin - tickLength * 0.5f - axisThickness * 0.5f,
                width = tickThickness,
                height = tickLength + axisThickness
            )

            val text = getTickText(xMinValue + xRange * (i.toFloat() / xAxisTickCount), isXAxis = true)
            surface.setDrawColor(tickTextColor, visibility)
            surface.drawText(text, xPos, yOrigin + tickTextSize, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = tickTextSize)
        }
    }

    private fun drawGraphLines(surface: Surface2D, engine: PulseEngine)
    {
        val entity = engine.scene.getEntity(entityIdToGraph)
        if (entity !is Graphable || entity.graphValues.isEmpty())
            return

        val values = entity.graphValues
        val xInvRange = 1f / (xMaxValue - xMinValue).coerceAtLeast(0.00000001f)
        val yInvRange = 1f / (yMaxValue - yMinValue).coerceAtLeast(0.00000001f)
        val xLeft = x - width * 0.5f
        val yTop = y - height * 0.5f

        // Draw Graph line
        surface.setDrawColor(lineColor, visibility)
        for (i in 1 until values.size)
        {
            val x0 = xLeft + width * (values[i - 1].x - xMinValue) * xInvRange
            val y0 = yTop + height * (1f - (values[i - 1].y - yMinValue) * yInvRange)
            val x1 = xLeft + width * (values[i].x - xMinValue) * xInvRange
            val y1 = yTop + height * (1f - (values[i].y - yMinValue) * yInvRange)

            if (lineThickness == 1f)
                surface.drawLine(x0, y0, x1, y1)
            else
            {
                val xCenter = (x0 + x1) * 0.5f
                val yCenter = (y0 + y1) * 0.5f
                val xDelta = x1 - x0
                val yDelta = y1 - y0
                val length = sqrt(xDelta * xDelta + yDelta * yDelta).coerceAtLeast(1f)
                val angle = -MathUtil.atan2(yDelta / length, xDelta / length).toDegrees()
                surface.drawTexture(
                    texture = Texture.BLANK,
                    x = xCenter,
                    y = yCenter,
                    width = lineThickness + length,
                    height = lineThickness,
                    rot = angle,
                    xOrigin = 0.5f,
                    yOrigin = 0.5f,
                    cornerRadius = lineThickness / 2f
                )
            }
        }

        // Update max values if last value is outside of graph range
        val lastValue = values.last()
        val xLast = lastValue.x
        val yLast = lastValue.y
        if (xLast > xMaxValue) xMaxValue = xLast
        if (yLast > yMaxValue) yMaxValue = yLast
    }

    private fun drawPlotPoints(surface: Surface2D, engine: PulseEngine)
    {
        val entity = engine.scene.getEntity(entityIdToPlot)
        if (entity !is Plottable || entity.plotPoints.isEmpty())
            return

        val points = entity.plotPoints
        val xInvRange = 1f / (xMaxValue - xMinValue).coerceAtLeast(1f)
        val yInvRange = 1f / (yMaxValue - yMinValue).coerceAtLeast(1f)
        val xLeft = x - width * 0.5f
        val yTop = y - height * 0.5f

        surface.setDrawColor(plotPointColor, visibility)
        points.forEachFast()
        {
            val xPoint = it.x.coerceIn(xMinValue, xMaxValue)
            val yPoint = it.y.coerceIn(yMinValue, yMaxValue)
            val x0 = xLeft + width * ((xPoint - xMinValue) * xInvRange)
            val y0 = yTop + height * (1f - (yPoint - yMinValue) * yInvRange)

            surface.drawTexture(
                texture = Texture.BLANK,
                x = x0,
                y = y0,
                width = plotPointSize,
                height = plotPointSize,
                rot = 0f,
                xOrigin = 0.5f,
                yOrigin = 0.5f,
                cornerRadius = plotPointSize / 2f
            )

            val text = it.y.format()
            surface.setDrawColor(plotPointTextColor, visibility)
            surface.drawText(text, x0 + plotPointSize, y0 - plotPointSize, xOrigin = 0f, yOrigin = 0.5f, fontSize = plotPointTextsize)
        }
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
interface Graphable
{
    /** Values added to this list will be graphed. */
    val graphValues: MutableList<Point>
}

/**
 * Interface for entities to provide values that can be plotted by the [Graph] class.
 */
interface Plottable
{
    /** Values added to this list will be plotted. */
    val plotPoints: MutableList<Point>
}

/**
 * Class represents a 2D point.
 */
data class Point(
    val x: Float,
    val y: Float
)