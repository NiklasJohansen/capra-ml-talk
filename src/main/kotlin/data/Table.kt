package data

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.input.CursorType
import no.njoh.pulseengine.core.input.Mouse
import no.njoh.pulseengine.core.shared.primitives.Array2D
import no.njoh.pulseengine.core.shared.primitives.Color
import presentation.Graphable
import presentation.Point
import presentation.PresentationEntity
import tools.setDrawColor
import kotlin.math.max

/**
 * This entity represents a dataset as a table.
 * Can be queried from an input [Node] to determine the nodes output value.
 * Also used to get the actual target value for an output node while training.
 */
class Table : PresentationEntity(), Dataset, Graphable
{
    /** String representation of the dataset used to populate the dataset. */
    var values = ""
        set (value) { field = processValues(value) }

    /** The character used to separate each row in the dataset. */
    var rowSeparator = ';'

    /** The character used to separate each column of a row in the dataset. */
    var columnSeparator = ','

    /** Set true if the first row of the dataset contains column names. */
    var hasHeaders = true

    /** Holds the index of the currently selected table sample. */
    override var selectedSampleIndex = -1

    // Styling parameters of table
    var textSize = 24f
    var borderSize = 0.4f
    var textColor = Color(0, 0, 0)
    var tableColor = Color(255, 255, 255)
    var headerColor = Color(228, 233, 234)
    var selectedColor = Color(207, 247, 207)
    var gridColor = Color(0, 0, 0)

    /** Local fast lookup table of the dataset. */
    @JsonIgnore private var table = Array2D<String?>(1, 1)

    /** List of values available for graphing. */
    @JsonIgnore override val graphValues = mutableListOf<Point>()

    /**
     * Handle mouse input and manual selection of rows.
     */
    override fun onUpdate(engine: PulseEngine)
    {
        val xm = engine.input.xWorldMouse
        val ym = engine.input.yWorldMouse
        val rowHeight = height / table.height
        val xStart = x - width * 0.5f
        val yStart = y - height * 0.5f + (if (hasHeaders) rowHeight else 0f)
        val dataRowsHeight = height - (if (hasHeaders) rowHeight else 0f)

        if (xm > xStart && xm < xStart + width && ym > yStart && ym < yStart + dataRowsHeight)
        {
            engine.input.setCursor(CursorType.HAND)
            if (engine.input.wasClicked(Mouse.LEFT))
            {
                val index = ((ym - yStart) / rowHeight).toInt()
                selectedSampleIndex = if (index != selectedSampleIndex) index else -1
            }
        }
    }

    /**
     * Render the dataset as a table.
     */
    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        val rowHeight = height / table.height
        val colWidth = width / table.width
        val xStart = x - width * 0.5f
        val yStart = y - height * 0.5f

        // Table background
        surface.setDrawColor(tableColor, visibility)
        surface.drawTexture(Texture.BLANK, xStart, yStart, width, height)

        // Table header
        if (hasHeaders)
        {
            surface.setDrawColor(headerColor, visibility)
            surface.drawTexture(Texture.BLANK, xStart, yStart, width, rowHeight)
        }

        // Selected sample highlight
        if (selectedSampleIndex >= 0)
        {
            val index = selectedSampleIndex + (if (hasHeaders) 1 else 0)
            surface.setDrawColor(selectedColor, visibility)
            surface.drawTexture(Texture.BLANK, xStart, yStart + index * rowHeight, width + borderSize, rowHeight)
        }

        // Horizontal lines
        surface.setDrawColor(gridColor, visibility)
        for (yIndex in 0 until table.height + 1)
            surface.drawTexture(Texture.BLANK, xStart, yStart + yIndex * rowHeight, width + borderSize, borderSize)

        // Vertical lines
        for (xIndex in 0 until table.width + 1)
            surface.drawTexture(Texture.BLANK, xStart + xIndex * colWidth, yStart, borderSize, height + borderSize)

        // Text
        surface.setDrawColor(textColor, visibility)
        for (y in 0 until table.height)
        {
            val yPos = yStart + y * rowHeight + (rowHeight * 0.5f)
            for (x in 0 until table.width)
            {
                val xPos = xStart + x * colWidth + (colWidth * 0.5f)
                val text = table[x, y]
                if (text != null)
                    surface.drawText(text, xPos, yPos, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = textSize)
            }
        }
    }

    /**
     * Parses the [values] string and updates the local [table].
     */
    private fun processValues(values: String): String
    {
        val cleanValues = values.trim().replace("\n", "").replace("\r", "")
        val rows = cleanValues.split(rowSeparator).map { it.split(columnSeparator) }
        val nColumns = rows.maxOf { row -> row.size }
        if (nColumns != table.width || rows.size != table.height)
            table = Array2D(nColumns, rows.size)

        graphValues.clear()
        for ((yIndex, row) in rows.withIndex())
        {
            for ((xIndex, columnValue) in row.withIndex())
                table[xIndex, yIndex] = columnValue

            if (row.size > 1 && !(hasHeaders && yIndex == 0))
            {
                // Add the data from the two first columns as a graph value
                val x = row[0].toFloatOrNull() ?: 0f
                val y = row[1].toFloatOrNull() ?: 0f
                graphValues.add(Point(x, y))
            }
        }

        return cleanValues
    }

    /** Returns the value of the currently selected sample at the given column index. */
    override fun getAttributeValue(index: Int): Float
    {
        val rowIndex = (max(selectedSampleIndex, 0) + (if (hasHeaders) 1 else 0)).coerceIn(0, table.height - 1)
        val colIndex = index.coerceIn(0, table.width - 1)
        return table[colIndex, rowIndex]?.toFloatOrNull() ?: -1f
    }

    /** Returns the number of columns per row. */
    override fun getAttributeCount(): Int = table.width

    /** Returns the number of samples with actual data (ignores the header row). */
    override fun getSampleCount(): Int = table.height - if (hasHeaders) 1 else 0

    /** Returns true if the current selected sample index is the last sample in the dataset. */
    override fun isLastSampleSelected(): Boolean = (selectedSampleIndex == (getSampleCount() - 1))

    /** Selects the first sample by setting the selected sample index to 0. */
    override fun selectFirstSample() { selectedSampleIndex = 0 }

    /** Increases the selected sample index by one. Wraps over to zero on last index. */
    override fun selectNextSample() { selectedSampleIndex = (selectedSampleIndex + 1) % getSampleCount() }

    /** Decreases the selected sample index by one. Wraps over to the last row when crossing zero. */
    override fun selectPreviousSample()
    {
        selectedSampleIndex = if (selectedSampleIndex - 1 < 0) getSampleCount() - 1 else selectedSampleIndex - 1
    }

    /** Enables events to change selected samples. */
    override fun onEventMessage(engine: PulseEngine, eventMessage: String)
    {
        when (eventMessage)
        {
            "NEXT" -> selectNextSample()
            "PREVIOUS" -> selectPreviousSample()
        }
    }
}