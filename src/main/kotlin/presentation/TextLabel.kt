package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.shared.primitives.Color

/**
 * Simple text label entity.
 */
class TextLabel : SceneEntity()
{
    var text: String = "Text"
    var textSize = 72f
    var color = Color(1f, 1f, 1f)

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        surface.setDrawColor(color)
        surface.drawText(text, x, y, fontSize = textSize, xOrigin = 0.5f, yOrigin = 0.5f)
    }
}