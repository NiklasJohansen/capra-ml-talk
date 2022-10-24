package demos.driving

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneState
import presentation.EventListener
import presentation.PresentationEntity

class StateMachine : PresentationEntity()
{
    var carPoolId = -1L
    var rouletteWheelId = -1L
    var geneticAlgorithmId = -1L
    var stateChangeTimeMs = 1000L
    var randomCarCount = 2

    @JsonIgnore var state: State = IdleState(this)

    override fun onUpdate(engine: PulseEngine)
    {
        val nextState = state.update(engine)
        if (nextState !== state)
        {
            state = if (state !is WaitState && stateChangeTimeMs > 0) WaitState(this, nextState) else nextState
        }
    }

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        if (engine.scene.state != SceneState.STOPPED)
            return // Only draw StateMachine in editor

        surface.setDrawColor(0.1f,0.1f, 0.1f)
        surface.drawTexture(Texture.BLANK, x, y, width, height, xOrigin = 0.5f, yOrigin = 0.5f)
        surface.setDrawColor(1f,1f, 1f)
        surface.drawText("StateMachine", x, y - 10f, xOrigin = 0.5f, yOrigin = 0.5f, fontSize = 30f)
        surface.drawText("($id)", x, y + 15f, xOrigin = 0.5f, yOrigin = 0.5f)
    }

    override fun onEventMessage(engine: PulseEngine, eventMessage: String)
    {
        when (eventMessage)
        {
            "STOP" -> state = IdleState(this)
            "NO_DELAY" -> stateChangeTimeMs = 0L
            else -> state.handleEvent(engine, eventMessage)
        }
    }

    abstract class State(val machine: StateMachine) : EventListener
    {
        abstract fun update(engine: PulseEngine): State
        override fun handleEvent(engine: PulseEngine, eventMsg: String) { }
    }

    private class IdleState(machine: StateMachine) : State(machine)
    {
        private var nexState: State = this

        override fun update(engine: PulseEngine): State = nexState

        override fun handleEvent(engine: PulseEngine, eventMsg: String)
        {
            when (eventMsg)
            {
                "START" -> nexState = InitState(machine)
            }
        }
    }

    private class WaitState(machine: StateMachine, val nextState: State) : State(machine)
    {
        val startTime = System.currentTimeMillis()
        override fun update(engine: PulseEngine): State
        {
            return if (System.currentTimeMillis() - startTime >= machine.stateChangeTimeMs) nextState else this
        }
    }

    private class InitState(machine: StateMachine) : State(machine)
    {
        override fun update(engine: PulseEngine): State
        {
            val carPool = engine.scene.getEntityOfType<CarPool>(machine.carPoolId)
                ?: return IdleState(machine)

            carPool.handleEvent(engine, "START_NEXT_GEN")

            return DrivingState(machine)
        }
    }

    private class DrivingState(machine: StateMachine) : State(machine)
    {
        override fun update(engine: PulseEngine): State
        {
            val carPool = engine.scene.getEntityOfType<CarPool>(machine.carPoolId)
                ?: return IdleState(machine)

            val allCarsIsFinished = carPool.currentGenerationCarIds.all { carId ->
                engine.scene.getEntityOfType<Car>(carId)?.let { it.crashed || it.finished } ?: true
            }

            return if (allCarsIsFinished) SelectionState(machine) else this
        }
    }

    private class SelectionState(machine: StateMachine) : State(machine)
    {
        var hasSpunWheel = false

        override fun update(engine: PulseEngine): State
        {
            val rouletteWheel = engine.scene.getEntityOfType<RouletteWheel>(machine.rouletteWheelId)
                ?: return IdleState(machine)

            if (!hasSpunWheel)
            {
                if (machine.stateChangeTimeMs == 0L)
                    rouletteWheel.handleEvent(engine, "SPIN_INSTANT_STOP")
                else
                    rouletteWheel.handleEvent(engine, "SPIN")

                hasSpunWheel = true
            }
            else if (!rouletteWheel.isSpinning)
            {
                return CreateCarState(machine)
            }

            return this
        }
    }

    private class CreateCarState(machine: StateMachine) : State(machine)
    {
        override fun update(engine: PulseEngine): State
        {
            val ga = engine.scene.getEntityOfType<GeneticAlgorithm>(machine.geneticAlgorithmId) ?: return IdleState(machine)
            val carPool = engine.scene.getEntityOfType<CarPool>(machine.carPoolId) ?: return IdleState(machine)

            val carsLeft = carPool.spawnCount - carPool.nextGenerationCarIds.size
            if (carsLeft > machine.randomCarCount)
            {
                ga.handleEvent(engine, "GENERATE")
                ga.handleEvent(engine, "SUBMIT")
            }
            else ga.handleEvent(engine, "SUBMIT_RANDOM")

            return if (carsLeft <= 0) InitState(machine) else SelectionState(machine)
        }
    }
}