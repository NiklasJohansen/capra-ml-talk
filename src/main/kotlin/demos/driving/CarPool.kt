package demos.driving

import com.fasterxml.jackson.annotation.JsonIgnore
import demos.driving.Car.DrivingMode
import network.ActivationFunction
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneEntity
import no.njoh.pulseengine.core.scene.SceneState
import no.njoh.pulseengine.core.shared.primitives.Color
import presentation.EventListener
import tools.NetworkGenerator
import kotlin.random.Random.Default.nextFloat

/**
 * Responsible for creating and keeping track of current and next generation cars.
 */
class CarPool : SceneEntity(), EventListener
{
    /** The ID of a [Car] to be used as template when creating new [Car]s. */
    var templateCarId = -1L

    /** The ID of the first [Checkpoint] in which to spawn the [Car]s. */
    var startCheckpointId = -1L

    /** The spawn angle in degrees for the [Car]s. */
    var spawnAngleDegrees = 0f

    /** The initial number of [Car]s to spawn. */
    var spawnCount = 1

    @JsonIgnore var currentGeneration = 1
    @JsonIgnore var nextGenerationCarIds = mutableListOf<Long>()
    @JsonIgnore var currentGenerationCarIds = mutableListOf<Long>()

    fun addNextGenerationCar(engine: PulseEngine, color: Color, weights: FloatArray?)
    {
        // Creat new car from template
        val templateCar = engine.scene.getEntityOfType<Car>(templateCarId) ?: return
        val startCheckPoint = engine.scene.getEntityOfType<Checkpoint>(startCheckpointId) ?: return
        val car = templateCar.copy()
        car.drivingMode = DrivingMode.AUTO
        car.generation = currentGeneration
        car.x = startCheckPoint.x
        car.y = startCheckPoint.y
        car.rotation = spawnAngleDegrees
        car.color = color.copy()
        car.visibility = 1f
        car.drivingMode = DrivingMode.MANUAL
        engine.scene.addEntity(car)
        car.init(engine)

        // Configure network generator
        val generator = NetworkGenerator().apply {
            networkTemplate = "${templateCar.sensorCount},5,2"
            outputLayerFunction = ActivationFunction.TANH
            nodeSize = 20f
            nodeSpacing = 5f
            layerSpacing = 50f
            nodeTextSize = 10f
            connectionTextVisible = false
            dataSourceId = car.id
        }

        // Generate new network for car
        car.network = generator.generateNetwork(engine, 950f, 450f)
        weights?.let { car.network.setWeights(engine, it) }

        // Add ID of car to next generation
        nextGenerationCarIds.add(car.id)
    }

    override fun handleEvent(engine: PulseEngine, eventMessage: String)
    {
        when (eventMessage)
        {
            "START_NEXT_GEN" ->
            {
                if (nextGenerationCarIds.isEmpty())
                    createRandomGeneration(engine)
                startNextGeneration(engine)
            }
        }
    }

    private fun startNextGeneration(engine: PulseEngine)
    {
        // Destroy current generation
        for (carId in currentGenerationCarIds)
        {
            val car = engine.scene.getEntityOfType<Car>(carId) ?: continue
            car.set(DEAD)
            car.network.destroy(engine)
        }

        // Prepare next generation
        for (carId in nextGenerationCarIds)
        {
            val car = engine.scene.getEntityOfType<Car>(carId) ?: continue
            car.drivingMode = DrivingMode.AUTO
            // TODO: Move from parking space to spawn. Animate?
        }

        // Populate current generation
        currentGenerationCarIds.clear()
        currentGenerationCarIds.addAll(nextGenerationCarIds)
        nextGenerationCarIds.clear()
        currentGeneration++
    }

    private fun createRandomGeneration(engine: PulseEngine)
    {
        for (i in 0 until spawnCount)
        {
            val hue = nextFloat()
            val saturation = 0.7f + 0.3f * nextFloat()
            val luminance = 0.9f
            val c = java.awt.Color.getHSBColor(hue, saturation, luminance)
            val color = Color(c.red / 255f, c.green / 255f, c.blue / 255f)

            addNextGenerationCar(engine, color = color, weights = null)
        }
    }

    override fun onRender(engine: PulseEngine, surface: Surface2D)
    {
        if (engine.scene.state != SceneState.STOPPED)
            return // Only draw CarPool in editor

        surface.setDrawColor(0.1f,0.1f, 0.1f)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f)
        surface.setDrawColor(1f,1f, 1f)
        surface.drawText("Car Pool", x, y - 10f, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 30f)
        surface.drawText("($id)", x, y + 15f, xOrigin = 0.5f, yOrigin = 0.5f)
    }
}