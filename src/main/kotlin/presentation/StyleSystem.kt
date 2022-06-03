package presentation

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.api.Multisampling
import no.njoh.pulseengine.core.graphics.postprocessing.effects.VignetteEffect
import no.njoh.pulseengine.core.scene.SceneSystem
import no.njoh.pulseengine.core.shared.primitives.Color

/**
 * System to control global styling parameters.
 */
class StyleSystem : SceneSystem()
{
    /** Background color of the presentation. */
    var backgroundColor = Color(0.1f, 0.1f, 0.15f, 1f)

    /** Fill color of nodes at low activation values. */
    var nodeLowColor = Color(47, 68, 94)

    /** Fill color of nodes at high activation values. */
    var nodeHighColor = Color(94, 136, 188)

    /** Border color of nodes. */
    var nodeBorderColor = Color(7, 7, 7)

    /** Connection color (line between node) when weight is negative. */
    var connectionNegativeColor = Color(1f, 0f, 0f)

    /** Connection color (line between node) when weight is positive. */
    var connectionPositiveColor = Color(0f, 1f, 0f)

    /** Amount of multisampling to apply to main surface. Prevents jagged edges. */
    var multisampling = Multisampling.MSAA16

    /** Sets the strength of the vignette effect. Zero equals no vignette effect. */
    var vignette = 0f
        set (value) { field = value; vignetteUpdated = true }

    @JsonIgnore private var vignetteUpdated = false
    @JsonIgnore private var vignetteEffect: VignetteEffect? = null

    override fun onCreate(engine: PulseEngine)
    {
        // Create separate background surface to draw vignette on
        engine.gfx.createSurface(name = BG_SURFACE_NAME, zOrder = 50, backgroundColor = backgroundColor)
        engine.gfx.mainSurface.setBackgroundColor(1f, 1f, 1f, 0f)
    }

    override fun onUpdate(engine: PulseEngine)
    {
        engine.gfx.mainSurface.setMultisampling(multisampling)

        if (vignetteUpdated)
        {
            // Get or create effect
            vignetteEffect = vignetteEffect ?:
                VignetteEffect(vignette).also { engine.gfx.getSurface(BG_SURFACE_NAME)?.addPostProcessingEffect(it) }

            vignetteEffect?.strength = vignette
            vignetteUpdated = false
        }
    }

    override fun onDestroy(engine: PulseEngine)
    {
        vignetteEffect?.let {
            it.cleanUp()
            engine.gfx.getSurface(BG_SURFACE_NAME)?.removePostProcessingEffect(it)
        }
        engine.gfx.deleteSurface(BG_SURFACE_NAME)
    }

    companion object
    {
        private const val BG_SURFACE_NAME = "background"
    }
}