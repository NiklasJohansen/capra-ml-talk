package neuralnet

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.input.CursorType
import no.njoh.pulseengine.core.input.Mouse
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Array2D
import no.njoh.pulseengine.core.shared.primitives.Color
import kotlin.math.max

/**
 * This entity represents a dataset as a table.
 * Can be queried from an input [Node] to determine the nodes output value.
 * Also used to get the actual target value for an output node while training.
 */
class Dataset : SceneEntity()
{
    /** String representation of the dataset used to populate the dataset. Expects a comma separated sample per line. */
    var values = ""
        set (value) { field = value; updateTable(value) }

    /** Holds the index of the currently selected table row. */
    var selectedRowIndex = -1

    /** Set true if the first row of the dataset contains column names*/
    var hasHeaders = true

    // Styling parameters of table
    var textSize = 24f
    var borderSize = 0.4f
    var tableColor = Color(255, 255, 255)
    var headerColor = Color(228, 233, 234)
    var selectedColor = Color(207, 247, 207)

    /** Local fast lookup table of the dataset. */
    @JsonIgnore private var table = Array2D<String?>(1, 1)

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
                selectedRowIndex = if (index != selectedRowIndex) index else -1
            }
        }
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        val rowHeight = height / table.height
        val colWidth = width / table.width
        val xStart = x - width * 0.5f
        val yStart = y - height * 0.5f

        // Table background
        surface.setDrawColor(tableColor)
        surface.drawTexture(Texture.BLANK, xStart, yStart, width, height)

        // Table header
        if (hasHeaders)
        {
            surface.setDrawColor(headerColor)
            surface.drawTexture(Texture.BLANK, xStart, yStart, width, rowHeight)
        }

        // Selected row highlight
        if (selectedRowIndex >= 0)
        {
            val index = selectedRowIndex + (if (hasHeaders) 1 else 0)
            surface.setDrawColor(selectedColor)
            surface.drawTexture(Texture.BLANK, xStart, yStart + index * rowHeight, width + borderSize, rowHeight)
        }

        // Horizontal lines
        surface.setDrawColor(0f, 0f, 0f)
        for (yIndex in 0 until table.height + 1)
            surface.drawTexture(Texture.BLANK, xStart, yStart + yIndex * rowHeight, width + borderSize, borderSize)

        // Vertical lines
        for (xIndex in 0 until table.width + 1)
            surface.drawTexture(Texture.BLANK, xStart + xIndex * colWidth, yStart, borderSize, height + borderSize)

        // Text
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
    private fun updateTable(values: String)
    {
        val rows = values.split("\n").map { it.split(",") }
        val nColumns = rows.maxOf { row -> row.size }
        if (nColumns != table.width || rows.size != table.height)
            table = Array2D(nColumns, rows.size)
        for ((yIndex, row) in rows.withIndex())
        {
            for ((xIndex, columnValue) in row.withIndex())
            {
                table[xIndex, yIndex] = columnValue
            }
        }
    }

    /**
     * Returns the value of the currently selected row at the given column index.
     */
    fun getSelectedValueAsFloat(columnIndex: Int): Float
    {
        val rowIndex = (max(selectedRowIndex, 0) + (if (hasHeaders) 1 else 0)).coerceIn(0, table.height - 1)
        val colIndex = columnIndex.coerceIn(0, table.width - 1)
        return table[colIndex, rowIndex]?.toFloatOrNull() ?: -1f
    }

    /**
     * Returns the number of rows with actual data.
     */
    fun getRowCount(): Int =
        table.height - if (hasHeaders) 1 else 0
}