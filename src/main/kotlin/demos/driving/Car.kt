package demos.driving

import com.fasterxml.jackson.annotation.JsonIgnore
import data.DataSource
import demos.driving.Car.DrivingMode.*
import network.Network
import network.Node
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneState.RUNNING
import no.njoh.pulseengine.core.shared.primitives.Color
import no.njoh.pulseengine.core.shared.utils.Extensions.degreesBetween
import no.njoh.pulseengine.core.shared.utils.Extensions.toRadians
import no.njoh.pulseengine.modules.physics.BodyType
import no.njoh.pulseengine.modules.physics.BodyType.DYNAMIC
import no.njoh.pulseengine.modules.physics.ContactResult
import no.njoh.pulseengine.modules.physics.bodies.PhysicsBody
import no.njoh.pulseengine.modules.physics.bodies.PolygonBody
import no.njoh.pulseengine.modules.physics.shapes.PolygonShape.Companion.N_POINT_FIELDS
import no.njoh.pulseengine.modules.physics.shapes.PolygonShape.Companion.X
import no.njoh.pulseengine.modules.physics.shapes.PolygonShape.Companion.X_LAST
import no.njoh.pulseengine.modules.physics.shapes.PolygonShape.Companion.Y
import no.njoh.pulseengine.modules.physics.shapes.PolygonShape.Companion.Y_LAST
import no.njoh.pulseengine.modules.physics.shapes.RectangleShape
import no.njoh.pulseengine.modules.scene.entities.Wall
import presentation.PresentationEntity
import tools.animate
import tools.setDrawColor
import kotlin.math.*

/**
 * A physical car that can be driven by a nerual network.
 * The car has sensors to detect the distance to the nearby environment.
 */
class Car : PresentationEntity(), PolygonBody, DataSource
{
    /** The drivingMode determines how the car should be controlled. */
    var drivingMode = AUTO

    /** The steering direction in range -1.0 to 1.0. */
    var steering = 0f

    /** Throttle in range -1.0 to 1.0. */
    var throttle = 0f

    /** Max car speed. */
    var maxSpeed = 1400f

    /** The maximum turn angle of the front wheels in degrees (0 - 90). */
    var maxTurningAngle = 65f

    /** The amount of friction between the back wheels and the ground (0.0 - 1.0). */
    var backWheelFriction = 0.1f

    /** The amount of friction between the front wheels and the ground (0.0 - 1.0). */
    var frontWheelFriction = 0.1f

    /** The number of sensors. */
    var sensorCount = 10

    /** The max distance the sensors will detect other objects. */
    var sensorMaxRange = 1000f

    /** The angle between the first and the last sensor. */
    var sensorConeAngle = 180f

    // Styling properties
    var color = Color(1f, 0.2f, 0.2f)
    var textureName = ""
    var xTextureScale = 1f
    var yTextureScale = 1f
    var sensorVisibility = 1f
    var hihglighted = false

    // Physics object properties
    @JsonIgnore
    override val shape = RectangleShape()
    override var bodyType = DYNAMIC
    override var layerMask = 1
    override var collisionMask = 1
    override var restitution = 0.2f
    override var density = 1f
    override var friction = 0.4f
    override var drag = 0.07f

    // Properties related to the state of the car
    @JsonIgnore var fitness = 0f
    @JsonIgnore var lastFitness = 0f
    @JsonIgnore var ticksWithoutProgress = 0
    @JsonIgnore var generation = 0
    @JsonIgnore var crashed = false
    @JsonIgnore var finished = false
    @JsonIgnore var sensorValues = FloatArray(10)
    @JsonIgnore var network = Network.EMPTY

    override fun onUpdate(engine: PulseEngine)
    {
        if (drivingMode == AUTO)
        {
            val throttleNode = engine.scene.getEntityOfType<Node>(network.getOutputNodeId(0))
            val steeringNode = engine.scene.getEntityOfType<Node>(network.getOutputNodeId(1))
            if (throttleNode != null && steeringNode != null)
            {
                throttle = throttleNode.outputValue
                steering = steeringNode.outputValue
            }
        }
    }

    override fun onFixedUpdate(engine: PulseEngine)
    {
        updateCarPhysics()
    }

    private fun updateCarPhysics()
    {
        // Determine angle of car and front wheels
        val vehicleAngle = -rotation.toRadians() - PI.toFloat() * 0.5f
        val frontWheelAngle = vehicleAngle + steering * maxTurningAngle.toRadians()

        // Calculate acceleration vectors for front and back wheels
        val xBackAcc = cos(vehicleAngle) * throttle * maxSpeed
        val yBackAcc = sin(vehicleAngle) * throttle * maxSpeed
        val xFrontAcc = cos(frontWheelAngle) * throttle * maxSpeed * 0.3f
        val yFrontAcc = sin(frontWheelAngle) * throttle * maxSpeed * 0.3f

        // Apply acceleration to each wheel
        shape.applyAcceleration(0, xBackAcc, yBackAcc)
        shape.applyAcceleration(1, xBackAcc, yBackAcc)
        shape.applyAcceleration(2, xFrontAcc, yFrontAcc)
        shape.applyAcceleration(3, xFrontAcc, yFrontAcc)

        // Calculate friction coefficients
        val grip = 1f - abs(throttle)
        val backFrictionCoefficient = backWheelFriction * grip
        val frontFrictionCoefficient = frontWheelFriction

        // Apply friction to each wheel relative to vehicle direction and wheel angle
        applyFriction(0, vehicleAngle, backFrictionCoefficient)
        applyFriction(1, vehicleAngle, backFrictionCoefficient)
        applyFriction(2, frontWheelAngle, frontFrictionCoefficient)
        applyFriction(3, frontWheelAngle, frontFrictionCoefficient)
    }

    private fun applyFriction(pointIndex: Int, wheelAngle: Float, frictionCoefficient: Float)
    {
        // Get current and last wheel position
        val i = pointIndex * N_POINT_FIELDS
        val x = shape.points[i + X]
        val y = shape.points[i + Y]
        val xLast = shape.points[i + X_LAST]
        val yLast = shape.points[i + Y_LAST]

        // Calculate linear velocity of wheel
        var xVel = x - xLast
        var yVel = y - yLast
        val velocity = sqrt(xVel * xVel + yVel * yVel)
        if (velocity == 0f)
            return

        // Calculate velocity direction and wheel normal vector
        val xVelDir = xVel / velocity
        val yVelDir = yVel / velocity
        val xWheelNormal = cos(wheelAngle + PI.toFloat() * 0.5f)
        val yWheelNormal = sin(wheelAngle + PI.toFloat() * 0.5f)

        // Calculate the friction vector based on the dot product between the velocity direction and the wheel normal
        val dot = xVelDir * xWheelNormal + yVelDir * yWheelNormal
        val xFrictionDir = xWheelNormal * dot * frictionCoefficient
        val yFrictionDir = yWheelNormal * dot * frictionCoefficient

        // Apply friction vector
        xVel -= xFrictionDir
        yVel -= yFrictionDir

        // Set the velocity by updating the last wheel position
        shape.points[i + X_LAST] = x - xVel
        shape.points[i + Y_LAST] = y - yVel
    }

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        val i = if (engine.scene.state == RUNNING && bodyType == DYNAMIC) engine.data.interpolation else 0f
        val x = x * i + (1f - i) * shape.xCenterLast
        val y = y * i + (1f - i) * shape.yCenterLast
        val r = rotation + i * rotation.degreesBetween(shape.angleLast)

        drawAndUpdateSensors(engine, surface, x, y, r)
        drawCar(engine, surface, x, y, r)
    }

    private fun drawCar(engine: PulseEngine, surface: Surface2D, x: Float, y: Float, rotation: Float)
    {
        val w = width * xTextureScale
        val h = height * yTextureScale
        val texture = engine.asset.getOrNull(textureName) ?: Texture.BLANK
        val alpha = visibility * (if (crashed && !hihglighted) 0.5f else 1f)

        surface.setDrawColor(color, alpha)
        surface.drawTexture(texture, x, y, w, h, rotation, xOrigin = 0.5f, yOrigin = 0.5f)
    }

    private fun drawAndUpdateSensors(engine: PulseEngine, surface: Surface2D, x: Float, y: Float, rotation: Float)
    {
        if (crashed) return // Don't draw sensors for crashed cars

        // Make sure sensorValues array is correct size
        if (sensorCount != sensorValues.size && sensorCount > 0)
            sensorValues = FloatArray(sensorCount)

        val carAngle = rotation + 90f
        val stepAngle = sensorConeAngle / sensorCount
        val startAngle = carAngle + 0.5f * (sensorConeAngle - stepAngle)
        for (i in 0 until sensorCount)
        {
            val angle = startAngle - stepAngle * i
            val hitPoint = engine.scene.getFirstEntityAlongRayOfType<Wall>(x, y, angle, sensorMaxRange)
            if (hitPoint != null)
            {
                // Draw line and hit point
                surface.setDrawColor(0.8f, 0.8f, 0.8f, visibility * sensorVisibility)
                surface.drawLine(x, y, hitPoint.xPos, hitPoint.yPos)
                surface.setDrawColor(color, visibility * sensorVisibility)
                surface.drawTexture(Texture.BLANK, hitPoint.xPos, hitPoint.yPos, 5f, 5f, xOrigin = 0.5f, yOrigin = 0.5f, cornerRadius = 5f)

                // Store normalized distance to hit point
                sensorValues[i] = 1f - (hitPoint.distance / sensorMaxRange).coerceIn(0f, 1f)
            }
            else sensorValues[i] = 0f // No hit point
        }
    }

    override fun getAttributeCount(): Int = sensorValues.size

    override fun getAttributeValue(index: Int): Float
    {
        return if (index < sensorValues.size) sensorValues[index] else 0f
    }

    override fun onEventMessage(engine: PulseEngine, eventMessage: String)
    {
        when (eventMessage)
        {
            "AUTO" -> drivingMode = AUTO
            "MANUAL" -> drivingMode = MANUAL
            "SHOW_SENSORS" -> engine.animate(::sensorVisibility, target = 1f)
            "HIDE_SENSORS" -> engine.animate(::sensorVisibility, target = 0f)
            "FORWARD_ON" -> throttle = 1f
            "FORWARD_OFF" -> if (throttle > 0) throttle = 0f
            "BACKWARD_ON" -> throttle = -1f
            "BACKWARD_OFF" -> if (throttle < 0) throttle = 0f
            "RIGHT_ON" -> steering = 1f
            "RIGHT_OFF" -> if (steering > 0) steering = 0f
            "LEFT_ON" -> steering = -1f
            "LEFT_OFF" -> if (steering < 0) steering = 0f
        }
    }

    override fun onCollision(engine: PulseEngine, otherBody: PhysicsBody, result: ContactResult)
    {
        setCrashed()
    }

    fun setCrashed()
    {
        crashed = true
        bodyType = BodyType.STATIC
        for (i in 0 until 4)
        {
            val index = i * N_POINT_FIELDS
            shape.points[index + X_LAST] = shape.points[index + X]
            shape.points[index + Y_LAST] = shape.points[index + Y]
        }
    }

    /**
     * Creates a new [Car] instance with the same attributes as this one.
     */
    fun copy(): Car
    {
        val c = Car()
        c.x = x
        c.y = y
        c.z = z
        c.width = width
        c.height = height
        c.rotation = rotation
        c.flags = flags
        c.drivingMode = drivingMode
        c.maxSpeed = maxSpeed
        c.maxTurningAngle = maxTurningAngle
        c.backWheelFriction = backWheelFriction
        c.frontWheelFriction = frontWheelFriction
        c.sensorCount = sensorCount
        c.sensorMaxRange = sensorMaxRange
        c.sensorConeAngle = sensorConeAngle
        c.color = color.copy()
        c.textureName = textureName
        c.xTextureScale  = xTextureScale
        c.yTextureScale = yTextureScale
        c.sensorVisibility = sensorVisibility
        c.bodyType = bodyType
        c.layerMask = layerMask
        c.collisionMask = collisionMask
        c.restitution = restitution
        c.density = density
        c.friction = friction
        c.drag = drag
        return c
    }

    enum class DrivingMode
    {
        /** A nural network controls the steering and throttle of the car. */
        AUTO,

        /** Manual input from e.g. events control the car. */
        MANUAL
    }
}