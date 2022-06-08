package data

import no.njoh.pulseengine.core.PulseEngine
import presentation.EventListener

/**
 * Provides functions to get data samples from a dataset.
 */
interface Dataset : DataSource, EventListener
{
    /** The index of the selected sample. */
    val selectedSampleIndex: Int

    /** Returns the number of samples in the dataset. */
    fun getSampleCount(): Int

    /** Returns true if the current selected sample is the last sample in the dataset. */
    fun isLastSampleSelected(): Boolean

    /** Selects the first sample of the dataset. */
    fun selectFirstSample()

    /** Sets the next sample as the selected one. */
    fun selectNextSample()

    /** Sets the previous sample as the selected one. */
    fun selectPreviousSample()

    /** Enables events to change selected samples. */
    override fun handleEvent(engine: PulseEngine, eventMessage: String)
    {
        when (eventMessage)
        {
            "NEXT" -> selectNextSample()
            "PREVIOUS" -> selectPreviousSample()
        }
    }
}