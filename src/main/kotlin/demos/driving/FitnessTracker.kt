package demos.driving

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.MathUtil
import no.njoh.pulseengine.core.shared.utils.MathUtil.pointToLineSegmentDistanceSquared
import presentation.PresentationEntity
import tools.mapToLongArray
import tools.setDrawColor
import kotlin.math.sqrt

/**
 * Responsible for tracking and updating the fitness of [Car]s.
 */
class FitnessTracker : PresentationEntity()
{
    /** A comma separated string of car IDs to track fitness for. */
    var carIds = ""

    /** The ID of the [CarPool] to source cars to track fitness for. */
    var carPoolId = -1L

    /** The index of the first [Checkpoint] in the path. */
    var startCheckpointIndex = 0

    /** The max index of the [Checkpoint]s in the path. */
    var endCheckpointIndex = 1000

    /** The number of seconds without any progress before a car is 'crashed'. */
    var maxSecondsWithoutProgress = 3

    // Styling parameters
    var backgroundColor = Color(220, 220, 220)
    var borderColor = Color(0, 0, 0)
    var textColor = Color(0, 0, 0)
    var textSize = 30f
    var borderWidth = 0f
    var cornerRadius = 10f

    @JsonIgnore private var path = emptyArray<Point>()
    @JsonIgnore private var carIdArray = LongArray(0)
    @JsonIgnore private var highestPerformingCarId = -1L
    @JsonIgnore private var highestFitness = 0f

    override fun onStart(engine: PulseEngine)
    {
        // Create path from checkpoints
        path = engine.scene.getAllEntitiesOfType<Checkpoint>()
            ?.filter { it.index in startCheckpointIndex..endCheckpointIndex }
            ?.sortedBy { it.index }
            ?.map { Point(it.x, it.y, 0f) }
            ?.toTypedArray()
            ?: emptyArray()

        // Calculate distance from start for each path point
        var distFromStart = 0f
        for (i in 1 until path.size)
        {
            val p0 = path[i - 1]
            val p1 = path[i]
            distFromStart += sqrt((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y))
            p1.distanceFromStart = distFromStart
        }

        // Create car ID array
        carIdArray = carIds.split(",").mapToLongArray { it.toLongOrNull() ?: -1 }
    }

    override fun onUpdate(engine: PulseEngine)
    {
        if (path.size < 2)
            return // No path to calculate score from

        // Reset the ID to the highest performing car
        highestPerformingCarId = -1L
        highestFitness = 0f

        // Update fitness of all referenced cars
        for (carId in carIdArray)
            engine.scene.getEntityOfType<Car>(carId)?.updateFitness(engine)

        // Update fitness of cars in the CarPool
        val carPool = engine.scene.getEntityOfType<CarPool>(carPoolId) ?: return
        for (carId in carPool.currentGenerationCarIds)
            engine.scene.getEntityOfType<Car>(carId)?.updateFitness(engine)
    }

    private fun Car.updateFitness(engine: PulseEngine)
    {
        // Find the path segment closest to the car
        var minDistIndex = 0
        var minDistance = Float.MAX_VALUE
        var lastPoint = path[0]
        for (i in 1 until path.size)
        {
            val point = path[i]
            val distance = pointToLineSegmentDistanceSquared(x, y, lastPoint.x, lastPoint.y, point.x, point.y)
            if (distance < minDistance)
            {
                minDistIndex = i - 1
                minDistance = distance
            }
            lastPoint = point
        }

        // Calculate the closest point on the path segment to the car
        val p0 = path[minDistIndex]
        val p1 = path[minDistIndex + 1]
        val cp = MathUtil.closestPointOnLineSegment(x, y, p0.x, p0.y, p1.x, p1.y)
        val distanceFromP0ToClosestPoint = sqrt((cp.x - p0.x) * (cp.x - p0.x) + (cp.y - p0.y) * (cp.y - p0.y))

        // Calculate the fitness from the traveled distance along the path
        fitness = (p0.distanceFromStart + distanceFromP0ToClosestPoint).coerceAtLeast(2f)

        // Track the fitness of the best performing car
        if (fitness > highestFitness)
        {
            highestFitness = fitness
            highestPerformingCarId = id
        }

        // When the last checkpoint is reached - set the car to finished
        if (minDistIndex + 1 == path.lastIndex && minDistance < 10f)
            finished = true

        // Count ticks without progress
        ticksWithoutProgress++
        if (fitness - lastFitness > 50)
        {
            lastFitness = fitness
            ticksWithoutProgress = 0
        }

        // Crash car if it is not making progress
        if (ticksWithoutProgress > maxSecondsWithoutProgress * engine.config.fixedTickRate)
            setCrashed()
    }

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        // Draw border
        surface.setDrawColor(borderColor, visibility)
        surface.drawTexture(Texture.BLANK, x, y, width + borderWidth, height + borderWidth, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = cornerRadius)

        // Draw background
        surface.setDrawColor(backgroundColor, visibility)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = cornerRadius)

        // Draw Texts
        val carColor = engine.scene.getEntityOfType<Car>(highestPerformingCarId)?.color ?: textColor
        surface.setDrawColor(carColor, visibility)
        surface.drawText(highestFitness.toInt().toString(), x, y, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = textSize)
    }

    private data class Point(
        val x: Float,
        val y: Float,
        var distanceFromStart: Float
    )
}