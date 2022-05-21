package systems

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.graphics.api.Multisampling
import no.njoh.pulseengine.core.graphics.postprocessing.effects.VignetteEffect
import no.njoh.pulseengine.core.scene.SceneSystem
import no.njoh.pulseengine.core.shared.primitives.Color

/**
 * System to control visual style parameters.
 */
class StyleSystem : SceneSystem()
{
    /** Background color of the presentation*/
    var backgroundColor = Color(0.1f, 0.1f, 0.15f, 1f)

    /** Amount of multisampling to apply to main surface. Prevents jagged edges. */
    var multisampling = Multisampling.MSAA16

    /** Sets the strength of the vignette effect. Zero equals no vignette effect. */
    var vignette = 0f
        set (value) { field = value; vignetteUpdated = true }

    @JsonIgnore private var vignetteUpdated = false
    @JsonIgnore private var vignetteEffect: VignetteEffect? = null

    override fun onUpdate(engine: PulseEngine)
    {
        engine.gfx.mainSurface.setBackgroundColor(backgroundColor)
        engine.gfx.mainSurface.setMultisampling(multisampling)

        if (vignetteUpdated)
        {
            // Get or create effect
            vignetteEffect = vignetteEffect ?:
                VignetteEffect(vignette).also { engine.gfx.mainSurface.addPostProcessingEffect(it) }

            vignetteEffect?.strength = vignette
            vignetteUpdated = false
        }
    }

    override fun onDestroy(engine: PulseEngine)
    {
        vignetteEffect?.let {
            it.cleanUp()
            engine.gfx.mainSurface.removePostProcessingEffect(it)
        }
    }
}