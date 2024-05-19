package tools

import com.fasterxml.jackson.annotation.JsonIgnore
import data.DataSource
import network.*
import network.Connection.Companion.NOT_SET
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.input.MouseButton
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

    /** The size of the circular node border. */
    var nodeBorderSize = 10f

    /** Horizontal spacing between layers. */
    var layerSpacing = 100f

    /** Vertical spacing between nodes. */
    var nodeSpacing = 50f

    /** Whether connections text should be visible and editable or not. */
    var connectionTextVisible = true

    /** Whether node text should be visible and editable or not. */
    var nodeTextVisible = true

    /** Font size of node text. */
    var nodeTextSize = 28f

    /** Whether activation function marker on [Node] should be visible or not. */
    var nodeActivationFunctionVisible = true

    /** The width of the connection lines between nodes. */
    var connectionThickness = 10f

    /** Activation function to use in hidden layers. */
    var hiddenLayerFunction = ActivationFunction.TANH

    /** Activation function to use in output layers. */
    var outputLayerFunction = ActivationFunction.SIGMOID

    /** ID of the [DataSource] to configure the input and output nodes with. */
    var dataSourceId = -1L

    @JsonIgnore
    private var newConnection = createNewConnection()

    override fun onUpdate(engine: PulseEngine)
    {
        when
        {
            engine.scene.state != SceneState.STOPPED -> return // Only use tools in editor, not while scene is running
            engine.input.isPressed(MouseButton.RIGHT) -> beginNodeConnection(engine)
            engine.input.wasReleased(MouseButton.RIGHT) -> finishNodeConnection(engine)
            engine.input.wasClicked(Key.G) -> generateNetwork(
                engine = engine,
                xPos = engine.input.xWorldMouse,
                yPos = engine.input.yWorldMouse
            )
        }
    }

    override fun onRender(engine: PulseEngine)
    {
        if (engine.scene.state == SceneState.STOPPED)
            newConnection.onRender(engine, engine.gfx.mainSurface)
    }

    /**
     * Generates a network of [Node]s and [Connection]s from the set parameters.
     */
    fun generateNetwork(engine: PulseEngine, xPos: Float, yPos: Float): Network
    {
        val layers = mutableListOf<List<Node>>()
        val connections = mutableListOf<Connection>()
        val dataSource = engine.scene.getEntityOfType<DataSource>(dataSourceId)
        val layerDefinitions = networkTemplate
            .split(",")
            .map { it.split("+").mapNotNull { it.toIntOrNull() } }

        // Create layers in network
        for ((layerIndex, layerDef) in layerDefinitions.withIndex())
        {
            val currentLayer = mutableListOf<Node>()
            val baseNodeCount = layerDef.getOrNull(0) ?: 0
            val biasNodeCount = layerDef.getOrNull(1) ?: 0
            val totalLayerNodeCount = baseNodeCount + biasNodeCount
            val xNode = xPos + layerIndex * (nodeSize + layerSpacing)
            val yNode = yPos - 0.5f * (totalLayerNodeCount * (nodeSize + nodeSpacing))
            val isFirstLayer = (layerIndex == 0)
            val isLastLayer = (layerIndex == layerDefinitions.lastIndex)

            // Create nodes in layer
            for (nodeIndex in 0 until totalLayerNodeCount)
            {
                val isBiasNode = (nodeIndex >= baseNodeCount)
                val newNode = createNewNode()
                newNode.x = xNode
                newNode.y = yNode + nodeIndex * (nodeSize + nodeSpacing)
                newNode.outputValue = if (isBiasNode) 1.0f else 0.5f
                newNode.activationFunction = when
                {
                    isFirstLayer || isBiasNode -> ActivationFunction.NONE
                    isLastLayer -> outputLayerFunction
                    else -> hiddenLayerFunction
                }

                // Add node to scene to give it an ID
                engine.scene.addEntity(newNode)

                // Create connections from all nodes in previous layer to the newNode
                if (!isBiasNode && layers.isNotEmpty())
                    newNode.connectToPreviousLayer(engine, layers.last(), connections, totalLayerNodeCount)

                // Set attributes related to data source
                if (!isBiasNode && dataSource != null)
                {
                    newNode.dataSourceId = dataSourceId
                    if (isFirstLayer)
                        newNode.attributeValueIndex = nodeIndex
                    else if (isLastLayer)
                        newNode.targetValueIndex = dataSource.getAttributeCount() - (totalLayerNodeCount - nodeIndex)
                    else
                        newNode.dataSourceId = -1
                }

                // Call onStart on node of scene is already started
                if (engine.scene.state == SceneState.RUNNING)
                    newNode.onStart(engine)

                currentLayer.add(newNode)
            }

            layers.add(currentLayer)
        }

        return Network(
            nodeIds = layers.mapToArray { layer -> layer.mapToLongArray { node -> node.id } },
            connectionIds = connections.mapToLongArray { it.id }
        )
    }

    /**
     * Creates [Connection]s between this [Node] and the [Node]s in the previous layer.
     */
    private fun Node.connectToPreviousLayer(
        engine: PulseEngine,
        prevLayerNodes: List<Node>,
        connections: MutableList<Connection>,
        thisLayerSize: Int
    ) {
        for (prevNode in prevLayerNodes)
        {
            val c = createNewConnection()
            c.x = (prevNode.x + this.x) * 0.5f
            c.y = (prevNode.y + this.y) * 0.5f
            c.showText = connectionTextVisible
            c.editable = connectionTextVisible
            c.fromNodeId = prevNode.id
            c.toNodeId = this.id
            c.weight = getConnectionWeight(
                lastLayerSize = prevLayerNodes.size,
                thisLayerSize = thisLayerSize
            )
            engine.scene.addEntity(c)
            connections.add(c)
        }
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
     * Creates a new [Connection] with initial size, depth and weight values.
     */
    private fun createNewConnection() = Connection().apply()
    {
        z = 2f
        width = 20f
        height = 6f
        weight = 1f
        lineThickness = connectionThickness
        fromNodeId = NOT_SET
    }

    /**
     * Creates a new [Node] with some default values set.
     */
    private fun createNewNode() = Node().apply()
    {
        width = nodeSize
        height = nodeSize
        borderSize = nodeBorderSize
        textSize = nodeTextSize
        showText = nodeTextVisible
        editable = nodeTextVisible
        showActivationFunction = nodeActivationFunctionVisible
    }
}