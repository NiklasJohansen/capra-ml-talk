package tools

import com.fasterxml.jackson.annotation.JsonIgnore
import neuralnet.Connection
import neuralnet.Connection.Companion.NOT_SET
import neuralnet.Node
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.input.Mouse
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.scene.SceneSystem

/**
 * Tool to more easily create connections between nodes in the scene editor.
 */
class NetworkBuilderTool : SceneSystem()
{
    @JsonIgnore
    private var nextConnection = createNewConnection()

    override fun onUpdate(engine: PulseEngine)
    {
        // Only use tool in editor, not while scene is running
        if (engine.scene.state == SceneState.RUNNING)
            return

        if (engine.input.isPressed(Mouse.RIGHT))
        {
            var isMouseInsideAnyNode = false
            val xm = engine.input.xWorldMouse
            val ym = engine.input.yWorldMouse
            engine.scene.forEachEntityOfType<Node> { node ->
                if (node.isInside(xm, ym))
                {
                    isMouseInsideAnyNode = true
                    if (nextConnection.fromNodeId == NOT_SET)
                    {
                        nextConnection.fromNodeId = node.id
                    }
                    else if (node.id != nextConnection.fromNodeId)
                    {
                        nextConnection.toNodeId = node.id
                    }
                }
            }

            if (!isMouseInsideAnyNode)
                nextConnection.toNodeId = NOT_SET
        }
        else
        {
            if (nextConnection.fromNodeId > 0 && nextConnection.toNodeId > 0)
            {
                // Add connection to scene
                engine.scene.addEntity(nextConnection)

                // Create new connection
                nextConnection = createNewConnection()
            }

            // Reset connection IDs
            nextConnection.fromNodeId = NOT_SET
            nextConnection.toNodeId = NOT_SET
        }
    }

    override fun onRender(engine: PulseEngine)
    {
        if (engine.scene.state == SceneState.RUNNING)
            return

        nextConnection.onRender(engine, engine.gfx.mainSurface)
    }

    private fun createNewConnection() = Connection().apply {
        weight = 1f
        width = 20f
        height = 6f
        z = 2f
        yTextOffset = -8f
    }
}