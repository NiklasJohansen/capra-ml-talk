package network

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.scene.SceneEntity.Companion.DEAD
import tools.mapToArray
import tools.mapToLongArray
import kotlin.math.min

/**
 * This class holds weak references (IDs) to the [Node]s and [Connection]s in a neural network.
 * The actual node and connection instances can be looked up in the active scene.
 */
data class Network(
    /** Contains the IDs of [Node]s in each layer. */
    val nodeIds: Array<LongArray>,

    /** Contains the IDs of all [Connections]s in the nettwork. */
    val connectionIds: LongArray
) {
    /** Returns the IDs of all nodes in the first layer. */
    fun getInputNodeIds(): LongArray = nodeIds.firstOrNull() ?: EMPTY_IDS

    /** Returns the IDs of all nodes in the last layer. */
    fun getOutputNodeIds(): LongArray = nodeIds.lastOrNull() ?: EMPTY_IDS

    /** Returns the ID of the node in the first layer with the given index. */
    fun getInputNodeId(index: Int) = nodeIds.firstOrNull()?.getOrNull(index) ?: INVALID_ID

    /** Returns the ID of the node in the last layer with the given index. */
    fun getOutputNodeId(index: Int) = nodeIds.lastOrNull()?.getOrNull(index) ?: INVALID_ID

    /** Returns an array containing the weights of the network. */
    fun getWeights(engine: PulseEngine): FloatArray =
        FloatArray(connectionIds.size) { engine.scene.getEntityOfType<Connection>(connectionIds[it])?.weight ?: 0f }

    /** Updates the network connections with the given weights. */
    fun setWeights(engine: PulseEngine, weights: FloatArray)
    {
        val size = min(weights.size, connectionIds.size)
        for (i in 0 until size)
            engine.scene.getEntityOfType<Connection>(connectionIds[i])?.weight = weights[i]
    }

    /** Computes the output value for all nodes in the network. */
    fun compute(engine: PulseEngine) {
        for (layer in nodeIds)
            for (nodeId in layer)
                engine.scene.getEntityOfType<Node>(nodeId)?.computeOutputValue(engine)
    }

    /** Destroys the network by killing all [Node] and [Connection] entities. */
    fun destroy(engine: PulseEngine)
    {
        // Remove connections
        for (connectionId in connectionIds)
            engine.scene.getEntityOfType<Connection>(connectionId)?.set(DEAD)

        // Remove nodes
        for (layerIds in nodeIds)
            for (nodeId in layerIds)
                engine.scene.getEntityOfType<Node>(nodeId)?.set(DEAD)
    }

    companion object
    {
        private const val INVALID_ID = -1L
        private val EMPTY_IDS = LongArray(0)

        /** Reference to an immutable empty [Network] */
        val EMPTY = Network(emptyArray(), EMPTY_IDS)

        fun generateFromDatasetId(engine: PulseEngine, datasetId: Long): Network
        {
            // Find output nodes
            val outputLayer = mutableListOf<Node>()
            engine.scene.forEachEntityOfType<Node> { node ->
                if (node.dataSourceId == datasetId && node.targetValueIndex >= 0)
                    outputLayer.add(node)
            }

            // Find subsequent nodes and create network layers
            val connections = mutableListOf<Connection>()
            val layers = mutableListOf(outputLayer)
            var lastLayer = outputLayer
            var thisLayer = mutableListOf<Node>()
            while (true)
            {
                // Find each node connected to the last layer
                engine.scene.forEachEntityOfType<Connection> { connection ->
                    // Is connection connected to any of the node in the last layer
                    if (lastLayer.any { node -> node.id == connection.toNodeId })
                    {
                        val fromNode = engine.scene.getEntityOfType<Node>(connection.fromNodeId)
                        if (fromNode != null && fromNode !in thisLayer)
                            thisLayer.add(fromNode)

                        connections.add(connection)
                    }
                }

                if (thisLayer.isEmpty())
                    break // No more nodes in network

                layers.add(0, thisLayer)
                lastLayer = thisLayer
                thisLayer = mutableListOf()
            }

            return Network(
                nodeIds = layers.mapToArray { it.mapToLongArray { it.id } },
                connectionIds = connections.mapToLongArray { it.id }
            )
        }
    }
}