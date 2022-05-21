package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.SceneState
import kotlin.math.min

/**
 * Spatial target to move the camera at a certain slide index
 */
class CameraTarget : SceneEntity()
{
    /** Indicates at what slide index this target i relevant. */
    var slideIndex = 0

    /** Defines the target zoom level the camera should be at when tracking this target. */
    var zoom = 1f

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        // Don't show markers when scene is running
        if (engine.scene.state == SceneState.RUNNING)
            return

        surface.setDrawColor(1f, 0.8f, 0f, 1f)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = min(width, height) * 0.5f)
        surface.setDrawColor(0f, 0f, 0f)
        surface.drawText(slideIndex.toString(), x, y, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 14f)
    }
}