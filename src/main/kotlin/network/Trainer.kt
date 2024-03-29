package network

import com.fasterxml.jackson.annotation.JsonIgnore
import data.Dataset
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.forEachFast
import no.njoh.pulseengine.core.shared.utils.Extensions.toRadians
import presentation.EventListener
import presentation.Graphable
import presentation.Point
import presentation.PresentationEntity
import tools.nextRandomGaussian
import tools.setDrawColor
import kotlin.math.*

/**
 * Neural network trainer implementing the backpropagation algorithm.
 */
class Trainer : PresentationEntity(), Graphable, EventListener
{
    /** Set true to start training. */
    var trainNetwork = false

    /** The ID of the [Dataset] to source samples from. */
    var datasetId = -1L

    /** The number of training iterations to perform each second. */
    var iterationsPerSecond = 2f

    /** The amount of weight correction to apply to connections for each epoch (1.0 = 100%). */
    var learningRate = 0.1f

    /** The amount of previous weight correction to carry over to next training epoch. */
    var momentum = 0.8f

    /** How many samples to accumulate corrections for before corrections are applied to the weight. */
    var batchSize = 100

    // Styling parameters
    var showAdjustmentArrows = false
    var arrowTextureName = ""
    var arrowSize = 50f
    var positiveArrowColor = Color(0f, 1f, 0f)
    var negativeArrowColor = Color(1f, 0f, 0f)

    @JsonIgnore var epoch = 0
    @JsonIgnore var trainedBatches = 0
    @JsonIgnore var meanSquaredError = 0f
    @JsonIgnore private var networkLayers = listOf<List<Node>>()
    @JsonIgnore private var outgoingConnections = mutableMapOf<Long, MutableList<Connection>>()
    @JsonIgnore private var nodeError = mutableMapOf<Long, Float>()
    @JsonIgnore private var accumulatedWeightCorrection = mutableMapOf<Long, Float>()
    @JsonIgnore private var lastWightCorrection = mutableMapOf<Long, Float>()
    @JsonIgnore private var lastTrainingIterationTime = 0.0
    @JsonIgnore private var accumulatedTime = 0.0
    @JsonIgnore private var accumulatedSquaredError = 0f
    @JsonIgnore private var trainedSamples = 0
    @JsonIgnore override val graphValues = mutableListOf<Point>()

    override fun onStart(engine: PulseEngine)
    {
        super.onStart(engine)

        networkLayers = buildLocalCachedNetwork(engine)
        lastTrainingIterationTime = System.currentTimeMillis().toDouble()
        accumulatedTime = 0.0
    }

    override fun onUpdate(engine: PulseEngine)
    {
        // Calculate the amount of time available for training
        val targetMillisBetweenIterations = 1000.0 / iterationsPerSecond
        accumulatedTime += System.currentTimeMillis().toDouble() - lastTrainingIterationTime

        // Perform as many training iterations as room for in the given time budget
        while (accumulatedTime >= targetMillisBetweenIterations)
        {
            accumulatedTime = max(0.0, accumulatedTime - targetMillisBetweenIterations)
            if (trainNetwork && networkLayers.isNotEmpty())
                trainOneIteration(engine)
        }
        lastTrainingIterationTime = System.currentTimeMillis().toDouble()

        // Make sure nodes are not responsible for updating their own output values if any Trainer is active
        Node.updateNodeValues = true
        engine.scene.forEachEntityOfType<Trainer> { if (it.trainNetwork) Node.updateNodeValues = false }
    }

    /**
     * Calculates weight corrections for one sample of the dataset.
     */
    private fun trainOneIteration(engine: PulseEngine)
    {
        // Get dataset by ID
        val dataset = engine.scene.getEntityOfType<Dataset>(datasetId) ?: return
        val isLastSampleInDataset = dataset.isLastSampleSelected()
        val applyCorrections = isLastSampleInDataset || (trainedSamples >= batchSize)

        // Update output values for all nodes (forward propagation)
        networkLayers.forEachFast { layer ->
            layer.forEachFast { node ->
                node.computeOutputValue(engine)
            }
        }

        // Calculate difference between network output and target dataset value (compute loss function)
        val outputLayer = networkLayers.last()
        outputLayer.forEachFast { node ->
            val targetValue = dataset.getAttributeValue(node.targetValueIndex)
            val currentValue = node.outputValue
            val error = targetValue - currentValue
            accumulatedSquaredError += error * error
            nodeError[node.id] = error * node.activationFunction.computeDerivative(node.weightedSum)
        }

        // Start correcting weights from the second to last layer and backwards (backward propagation)
        for (layerIndex in (networkLayers.size - 2) downTo 0)
        {
            for (node in networkLayers[layerIndex])
            {
                // Calculate the weighted error for this node
                var sum = 0f
                node.forEachOutgoingConnection { conn -> sum += conn.weight * (nodeError[conn.toNodeId] ?: 0f) }
                nodeError[node.id] = sum * node.activationFunction.computeDerivative(node.weightedSum)

                // Correct each weight iteratively
                node.forEachOutgoingConnection { connection ->
                    // Accumulate weight corrections over each sample in dataset
                    val newCorrection = node.outputValue * (nodeError[connection.toNodeId] ?: 0f)
                    val accumulatedCorrection = accumulatedWeightCorrection[connection.id] ?: 0f
                    accumulatedWeightCorrection[connection.id] = accumulatedCorrection + newCorrection

                    // Only apply weight corrections when they have been accumulated for the defined amount of samples
                    if (applyCorrections)
                    {
                        // Calculate weight correction based on accumulated corrections and last correction
                        val currentCorrection = accumulatedWeightCorrection[connection.id] ?: 0f
                        val lastCorrection = lastWightCorrection[connection.id] ?: 0f
                        val weightCorrection = learningRate * currentCorrection + momentum * lastCorrection

                        // Apply weight correction and reset
                        connection.weight += weightCorrection
                        lastWightCorrection[connection.id] = weightCorrection
                        accumulatedWeightCorrection[connection.id] = 0f
                    }
                }
            }
        }

        trainedSamples++

        // Take note of every trained batch
        if (applyCorrections)
        {
            trainedBatches++
            meanSquaredError = accumulatedSquaredError / trainedSamples
            graphValues.add(Point(trainedBatches.toFloat(), meanSquaredError))
            accumulatedSquaredError = 0f
            trainedSamples = 0
        }

        // When we reach the last sample we update the epoch counter
        if (isLastSampleInDataset)
            epoch++

        dataset.selectNextSample() // Move on to the next sample/row
    }

    /**
     * Creates a local representation of the network for faster lookup of nodes and connections.
     * Starts by finding the output nodes with the same datasetId as this trainer is configured with.
     * Then builds a layered network structure from the output nodes and backwards.
     */
    private fun buildLocalCachedNetwork(engine: PulseEngine): List<List<Node>>
    {
        // Find output nodes
        val outputLayer = mutableListOf<Node>()
        engine.scene.forEachEntityOfType<Node> { node ->
            if (node.dataSourceId == datasetId && node.targetValueIndex >= 0)
                outputLayer.add(node)
        }

        // Find subsequent nodes and create network layers
        val network = mutableListOf(outputLayer)
        var lastLayer = outputLayer
        var thisLayer = mutableListOf<Node>()
        while (true)
        {
            // Find each node connected to the last layer
            engine.scene.forEachEntityOfType<Connection> { connection ->

                // Is connection connected to any of the node in the last layer
                if (lastLayer.any { node -> node.id == connection.toNodeId })
                {
                    engine.scene.getEntityOfType<Node>(connection.fromNodeId)?.let { fromNode ->
                        if (fromNode !in thisLayer)
                            thisLayer.add(fromNode)

                        // Cache outgoing connection from node
                        outgoingConnections.computeIfAbsent(fromNode.id) { mutableListOf() }.add(connection)
                    }
                }
            }

            if (thisLayer.isEmpty())
                break // No more nodes in network

            network.add(0, thisLayer)
            lastLayer = thisLayer
            thisLayer = mutableListOf()
        }

        return network
    }

    /**
     * Randomizes the connection weights using Xavier initialization.
     */
    private fun randomizeWeights()
    {
        for ((i, layer) in networkLayers.withIndex())
        {
            val nextLayerSize = networkLayers.getOrNull(i + 1)?.size ?: 0
            val variance = 2.0 / (layer.size + nextLayerSize)
            val standardDeviation = sqrt(variance).toFloat()
            for (node in layer)
                node.forEachOutgoingConnection { it.weight = nextRandomGaussian() * standardDeviation }
        }
    }

    /**
     * Resets the trainer and the weights of the network.
     */
    private fun resetNetwork(engine: PulseEngine)
    {
        randomizeWeights()
        graphValues.clear()
        nodeError.clear()
        accumulatedWeightCorrection.clear()
        lastWightCorrection.clear()
        accumulatedTime = 0.0
        epoch = 0
        trainedSamples = 0
        trainedBatches = 0
        engine.scene.getEntityOfType<Dataset>(datasetId)?.selectFirstSample()
    }

    private inline fun Node.forEachOutgoingConnection(action: (Connection) -> Unit)
    {
        outgoingConnections[this.id]?.forEachFast { action(it) }
    }

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        if (showAdjustmentArrows)
        {
            val arrowTexture = engine.asset.getOrNull(arrowTextureName) ?: Texture.BLANK
            for ((conId, correction) in lastWightCorrection)
            {
                val connection = engine.scene.getEntityOfType<Connection>(conId) ?: continue
                val color = if (correction < 0) negativeArrowColor else positiveArrowColor
                val angle = connection.rotation //+ if (correction < 0) -90f else 90f
                surface.setDrawColor(color, visibility)
                surface.drawTexture(
                    texture = arrowTexture,
                    x = connection.x + 0.5f * cos(-angle.toRadians()) * (connection.width + arrowSize),
                    y = connection.y + 0.5f * sin(-angle.toRadians()) * (connection.width + arrowSize),
                    width = arrowSize,
                    height = arrowSize,
                    rot = angle + if (correction < 0) -90f else 90f,
                    xOrigin = 0.5f,
                    yOrigin = 0.5f
                )
            }
        }

        if (engine.scene.state != SceneState.STOPPED)
            return // Only draw Trainer in editor

        surface.setDrawColor(0.1f,0.1f, 0.1f, visibility)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f)
        surface.setDrawColor(1f,1f, 1f, visibility)
        surface.drawText("Trainer", x, y - 10f, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 30f)
        surface.drawText("($id)", x, y + 15f, xOrigin = 0.5f, yOrigin = 0.5f)
    }

    /**
     * Handles incoming events messages.
     */
    override fun onEventMessage(engine: PulseEngine, eventMessage: String)
    {
        when (eventMessage)
        {
            "START" -> trainNetwork = true
            "STOP" -> {
                trainNetwork = false
                iterationsPerSecond = 1f
            }
            "RESET" -> resetNetwork(engine)
            "SPEED_UP" -> iterationsPerSecond *= 1.5f
        }
    }
}