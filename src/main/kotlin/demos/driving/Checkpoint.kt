package demos.driving

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.primitives.Color
import presentation.PresentationEntity
import tools.getFirstEntityOfType
import tools.setDrawColor
import kotlin.math.min

/**
 * Used to create a path for tracking the fitness/progress of a [Car].
 */
class Checkpoint : PresentationEntity()
{
    /** The index of where in the path this checkpoint lies. */
    var index = 0

    // Styling parameters
    var backgroundColor = Color(10, 255, 55)
    var borderColor = Color(0, 0, 0)
    var borderWidth = 2f

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        val markerRadius = min(width + borderWidth, height + borderWidth) * 0.5f
        val nextCheckpoint = engine.scene.getFirstEntityOfType<Checkpoint> { it.index == index + 1 }
        if (nextCheckpoint != null)
        {
            surface.setDrawColor(backgroundColor, visibility)
            surface.drawLine(x, y, nextCheckpoint.x, nextCheckpoint.y)
        }

        // Border
        surface.setDrawColor(borderColor, visibility)
        surface.drawTexture(Texture.BLANK, x, y, width + borderWidth, height + borderWidth, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = markerRadius)

        // Background
        surface.setDrawColor(backgroundColor, visibility)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = markerRadius)

        // Text
        surface.setDrawColor(0f, 0f, 0f, visibility)
        surface.drawText(index.toString(), x, y, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 10f)
    }
}