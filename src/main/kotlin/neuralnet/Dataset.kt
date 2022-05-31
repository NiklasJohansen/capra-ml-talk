package neuralnet

import no.njoh.pulseengine.core.PulseEngine
import presentation.EventListener

/**
 * Provides functions to access data in a dataset.
 */
interface Dataset : EventListener
{
    /** The index of the selected sample. */
    val selectedSampleIndex: Int

    /** Returns the value of the currently selected sample at the given column index. */
    fun getSelectedValueAsFloat(columnIndex: Int): Float

    /** Returns the number of samples in the dataset. */
    fun getSampleCount(): Int

    /** Returns the number of columns per row. */
    fun getColumnCount(): Int

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