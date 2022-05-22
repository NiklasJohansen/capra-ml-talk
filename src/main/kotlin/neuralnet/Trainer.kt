package neuralnet

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.utils.Extensions.forEachFast
import presentation.Graphable
import tools.nextRandomGaussian
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Neural network trainer implementing the backpropagation algorithm.
 * Accumulates weight corrections for the entire dataset before weights are updated (batch/offline learning).
 */
class Trainer : SceneEntity(), Graphable<Int, Float>
{
    /** Set true to start training. */
    var trainNetwork = false

    /** The ID of the [Dataset] to source samples from. */
    var datasetId = -1L

    /** The number of training iterations to perform each second. */
    var iterationsPerSecond = 2f

    /** The amount of weight correction to apply to connections for each epoch (1.0 = 100%). */
    var learningRate = 0.9f

    /** The amount of previous weight correction to carry over to next training epoch. */
    var momentum = 0.8f

    @JsonIgnore private var network = listOf<List<Node>>()
    @JsonIgnore private var outgoingConnections = mutableMapOf<Long, MutableList<Connection>>()
    @JsonIgnore private var nodeError = mutableMapOf<Long, Float>()
    @JsonIgnore private var accumulatedWeightCorrection = mutableMapOf<Long, Float>()
    @JsonIgnore private var lastWightCorrection = mutableMapOf<Long, Float>()
    @JsonIgnore private var lastTrainingIterationTime = 0L
    @JsonIgnore private var accumulatedTime = 0L
    @JsonIgnore private var accumulatedSquaredError = 0f
    @JsonIgnore private var meanSquaredError = 0f
    @JsonIgnore private var epoch = 0
    @JsonIgnore override val graphValues = mutableListOf<Pair<Int, Float>>()

    override fun onStart(engine: PulseEngine)
    {
        network = buildLocalCachedNetwork(engine)
        lastTrainingIterationTime = System.currentTimeMillis()
        accumulatedTime = 0
    }

    override fun onUpdate(engine: PulseEngine)
    {
        // Calculate the amount of time available for training
        val targetMillisBetweenIterations = (1000f / iterationsPerSecond).toLong()
        accumulatedTime += System.currentTimeMillis() - lastTrainingIterationTime

        // Perform as many training iterations as room for in the given time budget
        while (accumulatedTime >= targetMillisBetweenIterations)
        {
            accumulatedTime = max(0L, accumulatedTime - targetMillisBetweenIterations)
            if (trainNetwork && network.isNotEmpty())
                trainOneIteration(engine)
        }
        lastTrainingIterationTime = System.currentTimeMillis()

        // TODO: Replace with UI buttons
        if (engine.input.wasClicked(Key.R)) resetNetwork(engine)
        if (engine.input.wasClicked(Key.ENTER)) trainNetwork = !trainNetwork
        if (engine.input.wasClicked(Key.UP)) iterationsPerSecond += 10
        if (engine.input.wasClicked(Key.DOWN)) iterationsPerSecond = max(0f, iterationsPerSecond - 10f)
    }

    /**
     * Calculates weight corrections for one sample of the dataset.
     * Updates the connection weights when corrections for all samples have been accumulated.
     */
    private fun trainOneIteration(engine: PulseEngine)
    {
        // Get dataset by ID
        val dataset = engine.scene.getEntityOfType<Dataset>(datasetId) ?: return
        val isLastSampleInDataset = dataset.isLastRowSelected()

        // Update output values for all nodes (forward propagation)
        for (layer in network)
            for (node in layer)
                node.updateNodeValue(engine)

        // Calculate difference between network output and target dataset value (compute loss function)
        val outputLayer = network.last()
        for (node in outputLayer)
        {
            val targetValue = dataset.getSelectedValueAsFloat(node.targetValueIndex)
            val currentValue = node.outputValue
            val error = targetValue - currentValue
            accumulatedSquaredError += error * error
            nodeError[node.id] = error * node.activationFunction.computeDerivative(node.weightedSum)
        }

        // Start correcting weights from the second to last layer and backwards (backward propagation)
        for (layerIndex in (network.size - 2) downTo 0)
        {
            for (node in network[layerIndex])
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

                    // Only apply weight corrections when corrections have been accumulated for all samples (batch/offline learning)
                    if (isLastSampleInDataset)
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

        // When we reach the last sample we update the mean squared error and epoch counter
        if (isLastSampleInDataset)
        {
            meanSquaredError = accumulatedSquaredError / dataset.getRowCount()
            graphValues.add(epoch to meanSquaredError)
            accumulatedSquaredError = 0f
            epoch++
        }

        dataset.setNextRow() // Move on to the next sample/row
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
            if (node.datasetId == datasetId && node.targetValueIndex >= 0)
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
        for ((i, layer) in network.withIndex())
        {
            val nextLayerSize = network.getOrNull(i + 1)?.size ?: 0
            val variance = 2.0 / (layer.size + nextLayerSize)
            val standardDeviation = sqrt(variance)
            for (node in layer)
                node.forEachOutgoingConnection { it.weight = (nextRandomGaussian() * standardDeviation).toFloat() }
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
        accumulatedTime = 0
        epoch = 0
        engine.scene.getEntityOfType<Dataset>(datasetId)?.let { it.selectedRowIndex = 0 }
    }

    private inline fun Node.forEachOutgoingConnection(action: (Connection) -> Unit)
    {
        outgoingConnections[this.id]?.forEachFast { action(it) }
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        // TODO: Create buttons to start, stop and reset training

        surface.setDrawColor(0f, 0f, 0f)
        surface.drawText("Squared ERROR: $meanSquaredError", x, y, xOrigin = 0.5f, yOrigin = 0.5f)
        surface.drawText("TickRate: $iterationsPerSecond", x, y + 20, xOrigin = 0.5f, yOrigin = 0.5f)
        surface.drawText("Epoch: $epoch", x, y + 40, xOrigin = 0.5f, yOrigin = 0.5f)
    }
}