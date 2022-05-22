package tools

import com.fasterxml.jackson.annotation.JsonIgnore
import neuralnet.Connection
import neuralnet.Connection.Companion.NOT_SET
import neuralnet.Node
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.input.Mouse
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.SceneEntity.Companion.SELECTED
import no.njoh.pulseengine.core.scene.SceneState.STOPPED
import no.njoh.pulseengine.core.scene.SceneSystem
import presentation.TextLabel
import java.util.Random
import kotlin.math.abs

/**
 * Tool to more easily work with [Node] and [Connection] entities in the editor.
 */
class NetworkBuilderTool : SceneSystem()
{
    /** Distance before selected entity is snapped to the nearest axis. */
    var snapDistance = 10f

    @JsonIgnore
    private var newConnection = createNewConnection()

    override fun onUpdate(engine: PulseEngine)
    {
        if (engine.scene.state != STOPPED)
            return // Only use tools in editor, not while scene is running

        handleConnectionCreation(engine)
        handleBulkConnectionsOperations(engine)
        handleNodeSnapping<Node>(engine)
        handleNodeSnapping<Connection>(engine)
        handleNodeSnapping<TextLabel>(engine)
    }

    private fun handleConnectionCreation(engine: PulseEngine)
    {
        if (engine.input.isPressed(Mouse.RIGHT))
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
        else
        {
            if (newConnection.fromNodeId != NOT_SET && newConnection.toNodeId != NOT_SET)
            {
                val fromNode = engine.scene.getEntityOfType<Node>(newConnection.fromNodeId)
                val toNode = engine.scene.getEntityOfType<Node>(newConnection.toNodeId)
                if (fromNode != null && toNode != null)
                {
                    newConnection.x = (fromNode.x + toNode.x) * 0.5f
                    newConnection.y = (fromNode.y + toNode.y) * 0.5f
                }
                engine.scene.addEntity(newConnection)
                newConnection = createNewConnection()
            }

            // Reset connection IDs
            newConnection.fromNodeId = NOT_SET
            newConnection.toNodeId = NOT_SET
        }
    }

    private inline fun <reified T : SceneEntity> handleNodeSnapping(engine: PulseEngine)
    {
        // Snapping is only active when left mouse and left shift is pressed
        if (!(engine.input.isPressed(Mouse.LEFT) && engine.input.isPressed(Key.LEFT_SHIFT)))
            return

        val selectedEntity = engine.scene.getAllEntitiesOfType<T>()?.firstOrNull { it.isSet(SELECTED) } ?: return
        val searchWidth = selectedEntity.width * 10f
        val searchHeight = selectedEntity.height * 10f

        engine.scene.forEachEntityNearbyOfType<T>(selectedEntity.x, selectedEntity.y, searchWidth, searchHeight)
        {
            if (it.id != selectedEntity.id)
            {
                if (abs(it.x - selectedEntity.x) < snapDistance) selectedEntity.x = it.x
                if (abs(it.y - selectedEntity.y) < snapDistance) selectedEntity.y = it.y
            }
        }
    }

    private fun handleBulkConnectionsOperations(engine: PulseEngine)
    {
        val rClicked = engine.input.wasClicked(Key.R)
        val tClicked = engine.input.wasClicked(Key.T)
        if (!rClicked && !tClicked)
            return // Return if none of the keys were clicked

        engine.scene.forEachEntityOfType<Connection>()
        {
            when
            {
                it.isNot(SELECTED) -> { } // Ignore connection if it is not selected
                rClicked -> it.weight = random.nextGaussian().toFloat() * 0.5f
                tClicked -> it.showText = !it.showText
            }
        }
    }

    override fun onRender(engine: PulseEngine)
    {
        if (engine.scene.state == STOPPED)
            newConnection.onRender(engine, engine.gfx.mainSurface)
    }

    private fun createNewConnection() = Connection().apply {
        weight = 1f
        width = 20f
        height = 6f
        z = 2f
    }

    companion object
    {
        // java.util.Random provides .nextGaussian() function
        private val random = Random()
    }
}