package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.surface.Surface
import no.njoh.pulseengine.core.shared.primitives.Color
import tools.setDrawColor

/**
 * Simple decoration entity used to display a texture.
 */
class Decoration : PresentationEntity()
{
    var color = Color(1f, 1f, 1f)
    var textureName: String = ""
    var cornerRadius = 0f

    init { setNot(DISCOVERABLE) }

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface)
    {
        surface.setDrawColor(color, visibility)
        surface.drawTexture(
            texture = engine.asset.getOrNull(textureName) ?: Texture.BLANK,
            x = x,
            y = y,
            width = width,
            height = height,
            angle = rotation,
            xOrigin = 0.5f,
            yOrigin = 0.5f,
            cornerRadius = cornerRadius
        )
    }
}