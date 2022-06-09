package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.primitives.Color
import tools.setDrawColor

/**
 * Simple text label entity.
 */
class TextLabel : PresentationEntity()
{
    var text: String = "Text"
    var textSize = 72f
    var color = Color(1f, 1f, 1f)

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        surface.setDrawColor(color, visibility)
        surface.drawText(text, x, y, fontSize = textSize, xOrigin = 0.5f, yOrigin = 0.5f)
    }
}