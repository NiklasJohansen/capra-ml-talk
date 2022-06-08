package data

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.input.Mouse
import no.njoh.pulseengine.core.scene.SceneEntity
import kotlin.math.max

/**
 * This entity represent a dataset of images and can be used to manipulate and draw the images on screen.
 * It implements the [Dataset] interface and can be a source of data for input and output [Node]s.
 */
class ImageSet : SceneEntity(), Dataset
{
    /** The name of the dataset asset to source images/samples from. */
    var datasetAssetName: String = ""

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
            val xCell = ((xm - x) / width * dataset.imageWidth).toInt()
            val yCell = ((ym - y) / height * dataset.imageHeight).toInt()
            val pixelIndex = yCell * dataset.imageWidth + xCell
            var pixelValue = dataset.getPixelValue(selectedSampleIndex, pixelIndex)

            // Change the pixel value with left and right mouse keys
            if (engine.input.isPressed(Mouse.LEFT)) pixelValue += 0.2f
            if (engine.input.isPressed(Mouse.RIGHT)) pixelValue -= 0.5f

            dataset.setPixelValue(selectedSampleIndex, pixelIndex, pixelValue.coerceIn(0f, 1f))
        }
    }

    /**
     * Draw the selected image from the dataset.
     */
    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        // Get a reference to the dataset
        dataset = engine.asset.getOrNull(datasetAssetName) ?: EMPTY_DATASET

        val xStart = x - width * 0.5f
        val yStart = y - height * 0.5f
        val pixelWidth = width / dataset.imageWidth
        val pixelHeight = height / dataset.imageHeight
        val imageIndex = selectedSampleIndex.coerceIn(0, dataset.imageCount - 1)
        var pixelIndex = 0

        for (yIndex in 0 until dataset.imageHeight)
        {
            for (xIndex in 0 until dataset.imageWidth)
            {
                val v = 1f - dataset.getPixelValue(imageIndex, pixelIndex++) // Invert value to have 1.0=black and 0.0=white
                surface.setDrawColor(v, v, v)
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

    companion object
    {
        private val EMPTY_DATASET = EmptyImageDataset()
    }
}