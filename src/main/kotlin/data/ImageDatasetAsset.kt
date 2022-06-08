package data

import no.njoh.pulseengine.core.asset.types.Asset
import no.njoh.pulseengine.core.shared.primitives.Array2D

/**
 * Represents a dataset of images and labels loaded from disk.
 */
abstract class ImageDatasetAsset(
    name: String,
    fileName: String,
): Asset(name, fileName) {

    /** The number of images in the dataset. */
    var imageCount = 1

    /** The total number of pixels per image. */
    var pixelCount = 1

    /** The number of label values per image. Example: 10 classes representing 0-9 will have a labelCount of 10. */
    var labelCount = 0

    /** The number of horizontal pixels in each image. */
    var imageWidth = 1

    /** The number of vertical pixels in each image. */
    var imageHeight = 1

    /** The main array containing pixel data and labels for all images. */
    protected var dataset = Array2D(1, 1) { _, _ -> 0f }

    /**
     * Returns the pixel value at the given [pixelIndex] for a specific image with index [imageIndex].
     */
    fun getPixelValue(imageIndex: Int, pixelIndex: Int): Float =
        if (imageIndex < dataset.height && pixelIndex < pixelCount)
            dataset[pixelIndex, imageIndex]
        else 0f

    /**
     * Returns the label value with given [labelIndex] for a specific image with index [imageIndex].
     */
    fun getLabelValue(imageIndex: Int, labelIndex: Int): Float =
        if (imageIndex < dataset.height && pixelCount + labelIndex < dataset.width)
            dataset[pixelCount + labelIndex, imageIndex]
        else 0f

    /**
     * Sets the pixel value at the given [pixelIndex] for a specific image with index [imageIndex].
     */
    fun setPixelValue(imageIndex: Int, pixelIndex: Int, value: Float)
    {
        if (imageIndex < dataset.height && pixelIndex < pixelCount)
            dataset[pixelIndex, imageIndex] = value
    }

    /** 'Deletes' the data by resetting the dataset to a minimum size. */
    override fun delete()
    {
        dataset = Array2D(1, 1) { _, _ -> 0f }
        labelCount = 0
        imageCount = 1
        pixelCount = 1
        imageWidth = 1
        imageHeight = 1
    }
}

/***
 * Empty ImageDatasetAsset used as placeholder until actual dataset is loaded.
 */
class EmptyImageDataset : ImageDatasetAsset("", "")
{
    override fun load() { }
}