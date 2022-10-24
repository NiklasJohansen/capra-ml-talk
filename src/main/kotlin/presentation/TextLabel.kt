package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Font
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
    var fontName = ""
    var color = Color(1f, 1f, 1f)

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        val font = engine.asset.getOrNull<Font>(fontName)

        surface.setDrawColor(color, visibility)
        surface.drawText(text, x, y, font, textSize, rotation, xOrigin = 0.5f, yOrigin = 0.5f)
    }
}