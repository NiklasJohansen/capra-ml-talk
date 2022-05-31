package presentation

import no.njoh.pulseengine.core.PulseEngine

/**
 * Interface to enable entities to listen for events.
 */
interface EventListener
{
    /**
     * Called when an event is dispatched.
     * @param engine A reference to the main [PulseEngine] instance.
     * @param eventMessage The event message.
     */
    fun handleEvent(engine: PulseEngine, eventMessage: String)
}