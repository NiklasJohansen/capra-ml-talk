import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.PulseEngineGame
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

        val isSceneFileInSaveDirectory = engine.data.exists("scenes/nnp-0.scn")
        engine.scene.loadAndSetActive("scenes/nnp-0.scn", fromClassPath = !isSceneFileInSaveDirectory)
        engine.scene.start()
    }

    override fun onUpdate() { }
    override fun onRender() { }
    override fun onDestroy() { }
}