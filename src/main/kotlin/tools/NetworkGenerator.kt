package tools

import com.fasterxml.jackson.annotation.JsonIgnore
import neuralnet.ActivationFunction
import neuralnet.Connection
import neuralnet.Connection.Companion.NOT_SET
import neuralnet.Dataset
import neuralnet.Node
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.input.Mouse
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.scene.SceneSystem
import kotlin.math.sqrt

/**
 * Tool used in scene editor to generate neural networks of [Node]s and [Connection]s.
 */
class NetworkGenerator : SceneSystem()
{
    /** The template string to generate a network from
     *  Example: '2+1,2+1,1' will generate a neural network with 2 normal nodes and 1 bias node in the
     *  first and second layer, and a last layer with only 1 normal node.
     */
    var networkTemplate = "2+1,2+1,1"

    /** Diameter of each node. */
    var nodeSize = 64f

    /** Horizontal spacing between layers. */
    var layerSpacing = 100f

    /** Vertical spacing between nodes. */
    var nodeSpacing = 50f

    /** Whether connections text should be visible and editable or not. */
    var connectionTextVisible = true

    /** Whether node text should be visible and editable or not. */
    var nodeTextVisible = true

    /** Whether activation function marker on [Node] should be visible or not. */
    var nodeActivationFunctionVisible = true

    /** Activation function to use in hidden layers. */
    var hiddenLayerFunction = ActivationFunction.TANH

    /** Activation function to use in output layers. */
    var outputLayerFunction = ActivationFunction.SIGMOID

    /** ID of the dataset to configure the input and output nodes to source their values from. */
    var datasetId = -1L

    @JsonIgnore
    private var newConnection = createNewConnection()

    override fun onUpdate(engine: PulseEngine)
    {
        when
        {
            engine.scene.state != SceneState.STOPPED -> return // Only use tools in editor, not while scene is running
            engine.input.wasClicked(Key.G) -> generateNetwork(engine)
            engine.input.isPressed(Mouse.RIGHT) -> beginNodeConnection(engine)
            engine.input.wasReleased(Mouse.RIGHT) -> finishNodeConnection(engine)
        }
    }

    override fun onRender(engine: PulseEngine)
    {
        if (engine.scene.state == SceneState.STOPPED)
            newConnection.onRender(engine, engine.gfx.mainSurface)
    }

    /**
     * Generates a network of [Node]s and [Connection]s from the parameters.
     */
    private fun generateNetwork(engine: PulseEngine)
    {
        val layerDefinitions = networkTemplate
            .split(",")
            .map { it.split("+").mapNotNull { it.toIntOrNull() } }

        val xm = engine.input.xWorldMouse
        val ym = engine.input.yWorldMouse
        val dataset = engine.scene.getEntityOfType<Dataset>(datasetId)
        var lastLayerNodes = mutableListOf<Node>()

        for ((layerIndex, layerDef) in layerDefinitions.withIndex())
        {
            val baseNodeCount = layerDef.getOrNull(0) ?: 0
            val biasNodeCount = layerDef.getOrNull(1) ?: 0
            val totalLayerNodeCount = baseNodeCount + biasNodeCount
            val yStart = ym - 0.5f * (totalLayerNodeCount * (nodeSize + nodeSpacing))
            val thisLayerNodes = mutableListOf<Node>()

            for (nodeIndex in 0 until totalLayerNodeCount)
            {
                val isBiasNode = (nodeIndex >= baseNodeCount)
                val newNode = Node().apply()
                {
                    x = xm + layerIndex * (nodeSize + layerSpacing)
                    y = yStart + nodeIndex * (nodeSize + nodeSpacing)
                    width = nodeSize
                    height = nodeSize
                    showText = nodeTextVisible
                    editable = nodeTextVisible
                    showActivationFunction = nodeActivationFunctionVisible
                    outputValue = if (isBiasNode) 1.0f else 0.5f
                    activationFunction = when
                    {
                        layerIndex == 0 || isBiasNode -> ActivationFunction.NONE
                        layerIndex == layerDefinitions.lastIndex -> outputLayerFunction
                        else -> hiddenLayerFunction
                    }
                }

                // Add new node to scene
                engine.scene.addEntity(newNode)
                thisLayerNodes.add(newNode)

                if (!isBiasNode)
                {
                    // Set attributes related to dataset
                    if (dataset != null)
                    {
                        newNode.datasetId = datasetId
                        val columnCount = dataset.getColumnCount()
                        when (layerIndex)
                        {
                            0 -> newNode.attributeIndex = nodeIndex
                            layerDefinitions.lastIndex -> newNode.idealValueIndex = columnCount - (totalLayerNodeCount - nodeIndex)
                        }
                    }

                    // Create connections from newNode to all nodes in previous layerDef
                    for (lastNode in lastLayerNodes)
                    {
                        engine.scene.addEntity(createNewConnection().apply()
                        {
                            x = (lastNode.x + newNode.x) * 0.5f
                            y = (lastNode.y + newNode.y) * 0.5f
                            showText = connectionTextVisible
                            editable = connectionTextVisible
                            fromNodeId = lastNode.id
                            toNodeId = newNode.id
                            weight = getConnectionWeight(
                                lastLayerSize = lastLayerNodes.size,
                                thisLayerSize = totalLayerNodeCount
                            )
                        })
                    }
                }
            }

            lastLayerNodes = thisLayerNodes
        }
    }

    /**
     * Finds the starting and ending [Node]s to start a new [Connection] between.
     */
    private fun beginNodeConnection(engine: PulseEngine)
    {
        newConnection.toNodeId = NOT_SET
        val xm = engine.input.xWorldMouse
        val ym = engine.input.yWorldMouse
        engine.scene.forEachEntityOfType<Node> { node ->
            when
            {
                !node.isInside(xm, ym) -> { } // Ignore node if mouse is not inside it
                newConnection.fromNodeId == NOT_SET -> newConnection.fromNodeId = node.id
                newConnection.fromNodeId != node.id -> newConnection.toNodeId = node.id
            }
        }
    }

    /**
     * Adds the new [Connection] to the scene if it connects two [Node]s.
     */
    private fun finishNodeConnection(engine: PulseEngine)
    {
        val fromNode = engine.scene.getEntityOfType<Node>(newConnection.fromNodeId)
        val toNode = engine.scene.getEntityOfType<Node>(newConnection.toNodeId)
        if (fromNode == null || toNode == null)
        {
            newConnection.fromNodeId = NOT_SET
            newConnection.toNodeId = NOT_SET
            return // Connections do not have valid from and to IDs, ignore it
        }

        // Set the position of the connection to be directly between the two nodes
        newConnection.x = (fromNode.x + toNode.x) * 0.5f
        newConnection.y = (fromNode.y + toNode.y) * 0.5f

        engine.scene.addEntity(newConnection)
        newConnection = createNewConnection()
    }

    /**
     * Returns a randomized weight using Xavier initialization.
     */
    private fun getConnectionWeight(lastLayerSize: Int, thisLayerSize: Int): Float
    {
        val variance = 2.0 / (lastLayerSize + thisLayerSize)
        val standardDeviation = sqrt(variance).toFloat()
        return nextRandomGaussian() * standardDeviation
    }

    /**
     * Creates a new [Connection] with initial size, depth and weight values.
     */
    private fun createNewConnection() = Connection().apply()
    {
        weight = 1f
        width = 20f
        height = 6f
        z = 2f
        fromNodeId = NOT_SET
    }
}