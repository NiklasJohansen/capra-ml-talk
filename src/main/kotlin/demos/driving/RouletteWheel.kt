package demos.driving

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.sumByFloat
import presentation.PresentationEntity
import tools.setDrawColor
import kotlin.math.*
import kotlin.random.Random

/**
 * Responsible for showing the fitness of all [Car]s in a pie chart and randomly selecting two cars.
 */
class RouletteWheel : PresentationEntity()
{
    /** The ID of the [CarPool] to get [Car]s from. */
    var carPoolId: Long = -1

    /** Determines how fast the wheel slow down. */
    var wheelFriction = 0.05f

    /** Determines how fast the wheel is spun. */
    var spinAcceleration = 0.5f

    // Styling properties
    var backgroundColor = Color(100, 100, 100)
    var borderColor = Color(230, 220, 220)
    var arrowColor = Color(0, 0, 0)
    var borderWidth = 2f
    var arrowSize = 10f
    var arrowIndent = 5f
    var arrowVisibility = 1f
    var brightness = 1f

    @JsonIgnore private var angularVelocity = 0f
    @JsonIgnore private var wheelAngle = 1f
    @JsonIgnore var selectedId0 = -1L
    @JsonIgnore var selectedId1 = -1L

    override fun onFixedUpdate(engine: PulseEngine)
    {
        // Update the angle and velocity of the wheel
        wheelAngle += angularVelocity / 360
        angularVelocity *= 1f - wheelFriction
        if (angularVelocity < 0.001f)
            angularVelocity = 0f
    }

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        var selectedCar0: Car? = null
        var selectedCar1: Car? = null
        val radius = min(width, height) * 0.5f

        // Draw border
        surface.setDrawColor(borderColor, visibility)
        surface.drawTexture(Texture.BLANK, x, y, width + borderWidth, height + borderWidth, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = radius)

        // Draw background
        surface.setDrawColor(backgroundColor, visibility)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = radius)

        // Find current cars and calculate total fitness sum
        val carPool = engine.scene.getEntityOfType<CarPool>(carPoolId) ?: return
        val fitnessSum = carPool.currentGenerationCarIds
            .sumByFloat { id -> engine.scene.getEntityOfType<Car>(id)?.fitness ?: 0f }

        var currentAngle = 0f
        for (carId in carPool.currentGenerationCarIds)
        {
            val car = engine.scene.getEntityOfType<Car>(carId) ?: continue
            car.hihglighted = false
            if (car.fitness == 0f) continue // Ignore cars without score
            val fraction = car.fitness / fitnessSum
            val segmentAngle = fraction * PI.toFloat() * 2f

            // Check to see if the fixed points is within the current segment
            val selectionAngle = ((wheelAngle % (2 * PI)) + currentAngle) % (2 * PI)
            if (selectedCar0 == null && selectionAngle < 2 * PI && (selectionAngle + segmentAngle) > 2 * PI)
                selectedCar0 = car
            else if (selectedCar1 == null && selectionAngle < PI && (selectionAngle + segmentAngle) > PI)
                selectedCar1 = car

            // Set draw color for whole segment
            surface.setDrawColor(
                red = car.color.red * brightness,
                green = car.color.green * brightness,
                blue = car.color.blue * brightness,
                alpha = visibility
            )

            // Draw segment as multiple triangles to create fan shape
            val triangleCount = max((fraction * 90).toInt(), 1)
            val angleIncrement = segmentAngle / triangleCount
            for (i in 0 until triangleCount)
            {
                val angle = wheelAngle + currentAngle
                val x1 = x + radius * cos(-angle)
                val y1 = y + radius * sin(-angle)
                val x2 = x + radius * cos(-(angle + angleIncrement))
                val y2 = y + radius * sin(-(angle + angleIncrement))
                currentAngle += angleIncrement

                surface.drawTriangle(x, y, x1, y1, x2, y2)
            }
        }

        // Draw arrows on each side
        val x0 = x - radius + arrowIndent
        val x1 = x + radius - arrowIndent
        surface.setDrawColor(arrowColor, visibility * arrowVisibility)
        surface.drawTriangle(x0, y, x0 - arrowSize * 2, y - arrowSize, x0 - arrowSize * 2, y + arrowSize)
        surface.drawTriangle(x1, y, x1 + arrowSize * 2, y - arrowSize, x1 + arrowSize * 2, y + arrowSize)
        surface.setDrawColor(selectedCar1?.color ?: arrowColor, visibility * arrowVisibility)
        surface.drawTriangle(x0 - arrowSize, y, x0 - arrowSize * 2, y - arrowSize / 2, x0 - arrowSize * 2, y + arrowSize / 2)
        surface.setDrawColor(selectedCar0?.color ?: arrowColor, visibility * arrowVisibility)
        surface.drawTriangle(x1 + arrowSize, y, x1 + arrowSize * 2, y - arrowSize / 2, x1 + arrowSize * 2, y + arrowSize / 2)

        // Store selected cars
        selectedId0 = selectedCar0?.id ?: -1L
        selectedId1 = selectedCar1?.id ?: -1L

        // Make selected cars highlighted
        selectedCar0?.hihglighted = true
        selectedCar1?.hihglighted = true
    }

    private fun Surface2D.drawTriangle(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float)
    {
        drawQuadVertex(x0, y0)
        drawQuadVertex(x1, y1)
        drawQuadVertex(x2, y2)
        drawQuadVertex(x0, y0)
    }

    override fun onEventMessage(engine: PulseEngine, eventMessage: String)
    {
        when (eventMessage)
        {
            "SPIN" -> angularVelocity += spinAcceleration * (0.5f + 0.5f * Random.nextFloat())
        }
    }
}