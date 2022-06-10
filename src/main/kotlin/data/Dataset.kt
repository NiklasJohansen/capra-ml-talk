package data

/**
 * Provides functions to get data samples from a dataset.
 */
interface Dataset : DataSource
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
}