package examples

import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.PulseEngineGame
import no.njoh.pulseengine.core.asset.types.*

fun main() = PulseEngine.run(AssetExample::class)

class AssetExample : PulseEngineGame()
{
    private var frame = 0f

    override fun onCreate()
    {
        // Load plane text
        engine.asset.loadText("examples/assets/textAsset.txt", "textAsset")

        // Load texture
        engine.asset.loadTexture("examples/assets/textureAsset.png", "textureAsset")

        // Load all textures in folder
        engine.asset.loadAllTextures("examples/assets/")

        // Load sprite sheet and define cell count
        engine.asset.loadSpriteSheet("examples/assets/spriteSheetAsset.png", "spriteSheetAsset", 6, 1)

        // Load sound
        engine.asset.loadSound("examples/assets/soundAsset.ogg", "soundAsset")

        // Load font and define available font sizes
        engine.asset.loadFont("examples/assets/fontAsset.ttf", "fontAsset", floatArrayOf(72f))

        // Set tick rate to 1 for this example
        engine.config.fixedTickRate = 1
    }

    override fun onUpdate() { }

    override fun onFixedUpdate()
    {
        // Get loaded sound asset
        val soundAsset = engine.asset.get<Sound>("soundAsset")

        // Create sound source and play it
        val sourceId = engine.audio.createSource(soundAsset)
        engine.audio.play(sourceId)

        // Increase frame counter
        frame = (frame + 1) % 6
    }

    override fun onRender()
    {
        // Get loaded assets
        val text = engine.asset.get<Text>("textAsset")
        val font = engine.asset.get<Font>("fontAsset")
        val texture = engine.asset.get<Texture>("textureAsset")
        val spriteSheet = engine.asset.get<SpriteSheet>("spriteSheetAsset")

        // Draw text with given font
        engine.gfx.mainSurface.drawText(text.text, 200f, 130f, font)

        // Draw texture
        engine.gfx.mainSurface.drawTexture(texture, 10f, 20f, 250f, 250f)

        // Draw texture from sprite sheet
        engine.gfx.mainSurface.drawTexture(spriteSheet.getTexture(frame.toInt()), 880f, 110f, 60f, 60f)
    }

    override fun onDestroy() { }
}