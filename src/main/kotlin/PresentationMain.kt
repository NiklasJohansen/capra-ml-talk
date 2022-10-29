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
        engine.window.title = "Capra - Fag og Øl (03.11.2022) - Hva er maskinlæring, sånn egentlig?"
        engine.widget.add(CommandLine(), Profiler(), SceneEditor())

        // Load initial assets
        engine.asset.loadAllTextures("/assets")
        engine.asset.loadFont("/assets/source-sans-pro.regular.ttf", "font_normal", fontSize = 100f)
        engine.asset.loadFont("/assets/source-sans-pro.bold.ttf", "font_bold", fontSize = 100f)

        // Load first scene either from save directory or from classpath
        val startSceneFileName = "scenes/main.scn"
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