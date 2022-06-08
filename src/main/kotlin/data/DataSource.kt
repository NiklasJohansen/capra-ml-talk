package data

/**
 * General interface to provide attribute data for a network.
 */
interface DataSource
{
    /**
     * Returns the number of available attributes.
     */
    fun getAttributeCount(): Int

    /**
     * Returns the attribute at the current index.
     */
    fun getAttributeValue(index: Int = 0): Float
}