import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.PulseEngineGame
import no.njoh.pulseengine.core.input.CursorType
import no.njoh.pulseengine.widgets.cli.CommandLine
import no.njoh.pulseengine.widgets.editor.SceneEditor
import no.njoh.pulseengine.widgets.profiler.Profiler

fun main() = PulseEngine.run(PresentationMain::class)

class PresentationMain : PulseEngineGame()
{
    override fun onCreate()
    {
        engine.window.title = "Neural Network Presentation"
        engine.widget.add(CommandLine(), Profiler(), SceneEditor())

        // Load initial assets
        engine.asset.loadTexture("/assets/play.png", "play")
        engine.asset.loadTexture("/assets/pause.png", "pause")
        engine.asset.loadTexture("/assets/next.png", "next")
        engine.asset.loadTexture("/assets/reset.png", "reset")
        engine.asset.loadTexture("/assets/fast_forward.png", "fast_forward")

        // Load first scene either from save directory or from classpath
        val startSceneFileName = "scenes/nnp-0.scn"
        val isSceneFileInSaveDirectory = engine.data.exists(startSceneFileName)
        engine.scene.loadAndSetActive(startSceneFileName, fromClassPath = !isSceneFileInSaveDirectory)
        engine.scene.start()
    }

    override fun onUpdate()
    {
        // Set default cursor once every frame
        engine.input.setCursor(CursorType.ARROW)
    }

    override fun onRender() { }
    override fun onDestroy() { }
}