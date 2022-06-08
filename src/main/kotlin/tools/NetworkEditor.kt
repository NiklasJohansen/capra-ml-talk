package tools

import network.Connection
import network.Node
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Key
import no.njoh.pulseengine.core.input.Mouse
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.SceneEntity.Companion.SELECTED
import no.njoh.pulseengine.core.scene.SceneState.STOPPED
import no.njoh.pulseengine.core.scene.SceneSystem
import presentation.TextLabel
import kotlin.math.abs
import kotlin.math.max

/**
 * Tool edit and align [Node] and [Connection] entities in the editor.
 */
class NetworkEditor : SceneSystem()
{
    /** Distance before selected entity is snapped to the nearest axis. */
    var snapDistance = 10f

    /** How many nodes should be aligned after one another on each row. */
    var nodesPerRow = 1

    /** The spacing between nodes when aligned. */
    var nodeSpacing = 5f

    override fun onUpdate(engine: PulseEngine)
    {
        if (engine.scene.state != STOPPED)
            return // Only use tools in editor, not while scene is running

        handleNodeAlignment(engine)
        handlePositionSnapping<Node>(engine)
        handlePositionSnapping<Connection>(engine)
        handlePositionSnapping<TextLabel>(engine)
    }

    /**
     * Snaps the position of the entity to the closet axis of nearby entities.
     */
    private inline fun <reified T : SceneEntity> handlePositionSnapping(engine: PulseEngine)
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

    /**
     * Aligns nodes in rows and columns.
     */
    private fun handleNodeAlignment(engine: PulseEngine)
    {
        if (!engine.input.wasClicked(Key.A))
            return // A was not pressed, return

        // Find selected Nodes
        val selectedNodes = mutableListOf<Node>()
        engine.scene.forEachEntityOfType<Node> { if (it.isSet(SELECTED)) selectedNodes.add(it) }
        selectedNodes.sortBy { if (it.dataSourceId < 0) 20000 else it.attributeValueIndex + it.targetValueIndex }

        if (selectedNodes.isEmpty())
            return

        val xStart = selectedNodes[0].x
        val yStart = selectedNodes[0].y
        val nodesPerRow = max(nodesPerRow, 1)
        selectedNodes.forEachIndexed { i, node ->
            node.x = xStart + (i % nodesPerRow) * (node.width + nodeSpacing)
            node.y = yStart + (i / nodesPerRow) * (node.height + nodeSpacing)
        }
    }
}