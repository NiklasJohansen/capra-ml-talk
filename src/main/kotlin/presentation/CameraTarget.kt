package presentation

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.modules.scene.entities.Camera
import kotlin.math.min

/**
 * Spatial target to move the camera at a certain slide index.
 */
class CameraTarget : SceneEntity(), EventListener
{
    /** Defines the target zoom level for the camera when tracking this target. */
    var zoom = 1f

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        // Don't show markers when scene is running
        if (engine.scene.state == SceneState.RUNNING)
            return

        val border = 2f
        val radius = min(width + border, height + border) * 0.5f
        surface.setDrawColor(0f, 0f, 0f)
        surface.drawTexture(Texture.BLANK, x, y, width + border, height + border, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = radius)
        surface.setDrawColor(1f, 0.8f, 0f)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = radius)
        surface.setDrawColor(0f, 0f, 0f)
        surface.drawText(id.toString(), x, y, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 10f)
    }

    override fun handleEvent(engine: PulseEngine, eventMessage: String)
    {
        if (eventMessage == "TRACK")
        {
            engine.scene.getFirstEntityOfType<Camera>()?.let()
            {
                it.targetEntityId = this.id
                it.targetZoom = this.zoom
            }
        }
    }
}