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
        engine.window.title = "Capra ML-presentation 03.11.2022"
        engine.widget.add(CommandLine(), Profiler(), SceneEditor())

        // TODO: Remove
        engine.data.saveDirectory = "D:\\Users\\Niklas\\OneDrive\\Documents\\Projects\\neural-network-presentation\\src\\main\\resources"

        // Load initial assets
        engine.asset.loadAllTextures("/assets")
        engine.asset.loadFont("/assets/source-sans-pro.regular.ttf", "font_normal", fontSize = 100f)
        engine.asset.loadFont("/assets/source-sans-pro.bold.ttf", "font_bold", fontSize = 100f)

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