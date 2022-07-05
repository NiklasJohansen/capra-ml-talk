import demos.driving.Car
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
        engine.data.saveDirectory = "D:\\Users\\Niklas\\OneDrive\\Documents\\Projects\\neural-network-presentation\\src\\main\\resources"

        // Load initial assets
        engine.asset.loadTexture("/assets/play.png", "play")
        engine.asset.loadTexture("/assets/pause.png", "pause")
        engine.asset.loadTexture("/assets/next.png", "next")
        engine.asset.loadTexture("/assets/reset.png", "reset")
        engine.asset.loadTexture("/assets/fast_forward.png", "fast_forward")
        engine.asset.loadTexture("/assets/car.png", "car")

        // Load first scene either from save directory or from classpath
        val startSceneFileName = "scenes/ga_cars_training.scn" // scenes/ga_cars.scn" // "scenes/nnp-0.scn"
        val isSceneFileInSaveDirectory = engine.data.exists(startSceneFileName)
        engine.scene.loadAndSetActive(startSceneFileName, fromClassPath = !isSceneFileInSaveDirectory)
        engine.scene.start()

        engine.data.addMetric("Cars", "") { engine.scene.getAllEntitiesOfType<Car>()?.size?.toFloat() ?: 0f }
    }

    override fun onUpdate()
    {
        // Set default cursor once every frame
        engine.input.setCursor(CursorType.ARROW)
    }

    override fun onRender() { }
    override fun onDestroy() { }
}