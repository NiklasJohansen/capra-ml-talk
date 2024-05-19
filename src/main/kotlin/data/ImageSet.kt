package data

import com.fasterxml.jackson.annotation.JsonIgnore
import demos.mnist.MnistAsset
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.surface.Surface
import no.njoh.pulseengine.core.input.MouseButton
import no.njoh.pulseengine.core.shared.primitives.Color
import presentation.PresentationEntity
import tools.lerp
import tools.setDrawColor
import kotlin.math.max
import kotlin.math.sqrt

/**
 * This entity represent a dataset of images and can be used to manipulate and draw the images on screen.
 * It implements the [Dataset] interface and can be a source of data for input and output [Node]s.
 */
class ImageSet : PresentationEntity(), Dataset
{
    /** The name of the dataset asset to source images/samples from. */
    var datasetAssetName: String = ""

    /** The color if dark pixels. */
    var pixelLowColor = Color(0f, 0f, 0f)

    /** The color of lit pixels. */
    var pixelHighColor = Color(1f, 1f, 1f)

    /** The color of the image border. */
    var borderColor = Color(0f, 0f, 0f)

    /** The size of the image border. */
    var borderSize = 10f

    /** Holds the index of the currently selected image/sample. */
    override var selectedSampleIndex = 0

    /** Local reference to the loaded asset. */
    @JsonIgnore
    private var dataset: ImageDatasetAsset = EMPTY_DATASET

    /**
     * Handle input and manual editing of the dataset.
     */
    override fun onUpdate(engine: PulseEngine)
    {
        val xm = engine.input.xWorldMouse + width * 0.5f
        val ym = engine.input.yWorldMouse + height * 0.5f
        if (xm > x && xm < x + width && ym > y && ym < y + height)
        {
            val isLeftPressed = engine.input.isPressed(MouseButton.LEFT)
            val isRightPressed = engine.input.isPressed(MouseButton.RIGHT)
            if (!isLeftPressed && !isRightPressed)
                return // return if none of the mouse buttons is pressed

            val radius = 1
            val maxDist = sqrt(radius * radius + radius * radius.toFloat())
            val xCell = ((xm - x) / width * dataset.imageWidth).toInt()
            val yCell = ((ym - y) / height * dataset.imageHeight).toInt()
            for (y0 in -radius .. radius)
            {
                for (x0 in -radius .. radius)
                {
                    if (xCell + x0 >= 0 && xCell + x0 < dataset.imageWidth && yCell + y0 >= 0 && yCell + y0 < dataset.imageHeight)
                    {
                        val pixelIndex = (yCell + y0) * dataset.imageWidth + (xCell + x0)
                        var pixelValue = dataset.getPixelValue(selectedSampleIndex, pixelIndex)
                        val a = 1f - (sqrt( x0 * x0 + y0 * y0.toFloat()) / maxDist)
                        if (isLeftPressed)
                            pixelValue += 0.1f * a * a
                        else if (isRightPressed)
                            pixelValue -= 0.5f * a
                        dataset.setPixelValue(selectedSampleIndex, pixelIndex, pixelValue.coerceIn(0f, 1f))
                    }
                }
            }
        }
    }

    /**
     * Draw the selected image from the dataset.
     */
    override fun onDrawToScreen(engine: PulseEngine, surface: Surface)
    {
        // Get a reference to the dataset
        dataset = engine.asset.getOrNull<MnistAsset>(datasetAssetName) ?: EMPTY_DATASET

        val xStart = x - width * 0.5f
        val yStart = y - height * 0.5f
        val pixelWidth = width / dataset.imageWidth
        val pixelHeight = height / dataset.imageHeight
        val imageIndex = selectedSampleIndex.coerceIn(0, dataset.imageCount - 1)
        var pixelIndex = 0

        // Draw border
        surface.setDrawColor(borderColor, visibility)
        surface.drawQuad(xStart, yStart - borderSize, -borderSize, height + borderSize * 2f)
        surface.drawQuad(xStart + width, yStart - borderSize, borderSize, height + borderSize * 2f)
        surface.drawQuad(xStart - borderSize, yStart, width + borderSize * 2, -borderSize)
        surface.drawQuad(xStart - borderSize, yStart + height, width + borderSize * 2, borderSize)

        // Draw pixels
        for (yIndex in 0 until dataset.imageHeight)
        {
            for (xIndex in 0 until dataset.imageWidth)
            {
                val v = dataset.getPixelValue(imageIndex, pixelIndex++)
                val r = lerp(pixelLowColor.red, pixelHighColor.red, v)
                val g = lerp(pixelLowColor.green, pixelHighColor.green, v)
                val b = lerp(pixelLowColor.blue, pixelHighColor.blue, v)
                val a = lerp(pixelLowColor.alpha, pixelHighColor.alpha, v)
                surface.setDrawColor(r, g, b, a * visibility)
                surface.drawQuad(xStart + xIndex * pixelWidth, yStart + yIndex * pixelHeight, pixelWidth, pixelHeight)
            }
        }
    }

    /** Returns the value of the currently selected image at the given pixel index. */
    override fun getAttributeValue(index: Int): Float
    {
        val sampleIndex = (max(selectedSampleIndex, 0)).coerceIn(0, dataset.imageCount - 1)
        return if (index < dataset.pixelCount)
            dataset.getPixelValue(sampleIndex, index)
        else
            dataset.getLabelValue(sampleIndex, index - dataset.pixelCount)
    }

    /** Returns the number of attributes per sample (pixelCount + idealValues). */
    override fun getAttributeCount(): Int = dataset.pixelCount + dataset.labelCount

    /** Returns the number of images in the dataset. */
    override fun getSampleCount(): Int = dataset.imageCount

    /** Returns true if the current selected images is the last images in the dataset. */
    override fun isLastSampleSelected(): Boolean = selectedSampleIndex == dataset.imageCount - 1

    /** Selects the first image of the dataset. */
    override fun selectFirstSample() { selectedSampleIndex = 0 }

    /** Sets the next image as the selected one. */
    override fun selectNextSample() { selectedSampleIndex = (selectedSampleIndex + 1) % dataset.imageCount }

    /** Sets the selected image to the previous one. */
    override fun selectPreviousSample()
    {
        selectedSampleIndex = if (selectedSampleIndex - 1 < 0) dataset.imageCount - 1 else selectedSampleIndex - 1
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

    companion object
    {
        private val EMPTY_DATASET = EmptyImageDataset()
    }
}