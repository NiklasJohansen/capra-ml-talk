package demos.mnist

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.scene.SceneSystem
import no.njoh.pulseengine.core.shared.utils.Logger

/**
 * Simple system to configure loading of [MnistAsset] from disk.
 */
class MnistAssetLoader : SceneSystem()
{
    /** Asset name of the [MnistAsset] to be loaded. */
    var assetName = "mnist"
        set (value) { oldAssetName = field; field = value }

    /** The name of the file containing the MNIST labels. */
    var labelFileName = "/datasets/mnist/train-labels.idx1-ubyte"

    /** The name of the file containing the MNIST images. */
    var imageFileName = "/datasets/mnist/train-images.idx3-ubyte"

    /** The max number of images to load from the dataset. */
    var maxImagesToLoad = 1000

    /** Whether to reduce the image sizes to half their original size or not. */
    var reduceToHalfSize = true

    @JsonIgnore private var oldAssetName: String? = null

    override fun onStart(engine: PulseEngine)
    {
        if (oldAssetName != null && oldAssetName != assetName)
        {
            Logger.info("Deleting old asset: $oldAssetName")
            engine.asset.delete(oldAssetName!!)
            oldAssetName = null
        }

        val existingDataset = engine.asset.getOrNull<MnistAsset>(assetName)
        val reloadDataset =
            existingDataset == null ||
            maxImagesToLoad != existingDataset.maxImagesToLoad ||
            reduceToHalfSize != existingDataset.reduceToHalfSize

        if (reloadDataset)
        {
            Logger.info("Starting to load MNIST asset: $assetName from files: $labelFileName and $imageFileName")
            engine.asset.load(MnistAsset(assetName, labelFileName, imageFileName, maxImagesToLoad, reduceToHalfSize))
        }
    }
}