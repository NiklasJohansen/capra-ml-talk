package demos.mnist

import data.ImageDatasetAsset
import no.njoh.pulseengine.core.shared.primitives.Array2D
import no.njoh.pulseengine.core.shared.utils.Logger
import java.io.DataInputStream
import java.io.FileNotFoundException
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

/**
 * Implementation of the [ImageDatasetAsset] for loading the MNIST dataset.
 * @see http://yann.lecun.com/exdb/mnist/
 */
class MnistAsset(
    name: String,
    val labelFileName: String,
    val imageFileName: String,
    val maxImagesToLoad: Int,
    val reduceToHalfSize: Boolean = false
) : ImageDatasetAsset(name, labelFileName) {

    override fun load()
    {
        try
        {
            val time = measureTimeMillis { loadDatasetFromDisk() }
            Logger.info("Successfully loaded MNIST dataset from disk in $time ms")
        }
        catch (e: Exception) { Logger.error("Failed to load MNIST dataset from disk! Message: ${e.message}") }
    }

    private fun loadDatasetFromDisk()
    {
        val labels = javaClass.getResourceAsStream(labelFileName)?.let { DataInputStream(it) }
            ?: throw FileNotFoundException("Could not find label file: $labelFileName")
        val images = javaClass.getResourceAsStream(imageFileName)?.let { DataInputStream(it) }
            ?: throw FileNotFoundException("Could not find image file: $imageFileName")

        val labelsMagicNumber = labels.readInt()
        if (labelsMagicNumber != 2049)
            throw IllegalStateException("Magic number of $labelFileName is $labelsMagicNumber, but should be 2049")

        val imagesMagicNumber = images.readInt()
        if (imagesMagicNumber != 2051)
            throw IllegalStateException("Magic number of $imageFileName is $imagesMagicNumber, but should be 2051")

        val numLabels = labels.readInt()
        val numImages = images.readInt()
        if (numLabels != numImages)
            throw IllegalStateException("Number of labels: $numLabels do not match number of images: $images")

        imageHeight = images.readInt()
        imageWidth = images.readInt()
        pixelCount = imageHeight * imageWidth
        imageCount = min(numImages, maxImagesToLoad)
        labelCount = 10 // 10 classes (0-9)
        dataset = Array2D(width = pixelCount + labelCount, height = imageCount)

        // Read the label and pixel data from disk into memory
        val labelData = ByteArray(numLabels).also { labels.read(it) }
        val imageData = ByteArray(numImages * pixelCount).also { images.read(it) }

        var pixelIndex = 0
        for (imageIndex in 0 until imageCount)
        {
            // Insert pixel data as attributes
            for (i in 0 until pixelCount)
                dataset[i, imageIndex] = (imageData[pixelIndex++].toUByte()).toFloat() / 255.0f

            // Insert ideal values where the index corresponding to the label number (0-9) is set to 1.0 (One-hot encoding)
            val label = labelData[imageIndex].toInt()
            for (i in 0 until labelCount)
                dataset[pixelCount + i, imageIndex] = (if (i == label) 1.0f else 0.0f)
        }

        images.close()
        labels.close()

        if (reduceToHalfSize)
            halfImageSize()
    }

    /**
     * Updates the pixel data of the [dataset] to be half the original size.
     */
    private fun halfImageSize()
    {
        val newImageWidth = imageWidth / 2
        val newImageHeight = imageHeight / 2
        val newPixelCount = newImageWidth * newImageHeight
        val newDataset = Array2D<Float>(width = newPixelCount + labelCount, height = imageCount)

        for (imageIndex in 0 until imageCount)
        {
            for (y in 0 until imageHeight - 1 step 2)
            {
                for (x in 0 until imageWidth - 1 step 2)
                {
                    val v0 = dataset[y * imageWidth + x, imageIndex]
                    val v1 = dataset[y * imageWidth + x + 1, imageIndex]
                    val v2 = dataset[(y + 1) * imageWidth + x, imageIndex]
                    val v3 = dataset[(y + 1) * imageWidth + x + 1, imageIndex]

                    // Use the average of the two highest pixel values
                    val newPixelValue = (max(v0, v1) + max(v2, v3)) * 0.5f
                    val newPixelIndex = (y / 2) * newImageWidth + (x / 2)

                    newDataset[newPixelIndex, imageIndex] = newPixelValue
                }
            }

            // Transfer the label values
            for (j in 0 until labelCount)
                newDataset[newPixelCount + j, imageIndex] = dataset[pixelCount + j, imageIndex]
        }

        dataset = newDataset
        pixelCount = newPixelCount
        imageWidth = newImageWidth
        imageHeight = newImageHeight
    }
}

