package demos.driving

import com.fasterxml.jackson.annotation.JsonIgnore
import no.njoh.pulseengine.core.PulseEngine
import no.njoh.pulseengine.core.asset.types.Texture.Companion.BLANK
import no.njoh.pulseengine.core.graphics.Surface2D
import no.njoh.pulseengine.core.scene.SceneState.RUNNING
import no.njoh.pulseengine.core.shared.primitives.Color
import presentation.PresentationEntity
import tools.format
import tools.mapToFloatArray
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random.Default.nextFloat

/**
 * Responsible for showing the weights/genes of two parent car networks and for generating children cars with
 * a combination of the genes of the two parents.
 */
class GeneticAlgorithm : PresentationEntity()
{
    /** The ID of the father [Car]. */
    var fatherCarId = -1L

    /** The ID of the mother [Car]. */
    var motherCarId = -1L

    /** The ID of the [RouletteWheel] entity. Used to find the IDs of the selected cars. */
    var rouletteWheelId = -1L

    /** The ID of the [CarPool] to submit the new child cars to. */
    var carPoolId = -1L

    /** Determines the amount of genes (in percentage) to transfer from one of the parents when creating child genes. */
    var cutLengthPercentage = 30

    // Styling parameters
    var textColor = Color(1f, 1f, 1f)
    var textSize = 10f
    var showText = false
    var spacing = 2f
    var cornerRadius = 2f
    var parentRowSpacing = 10f
    var childRowSpacing = 10f

    @JsonIgnore var lastFatherCarId: Long? = null
    @JsonIgnore var lastMotherCarId: Long? = null
    @JsonIgnore var fatherGenes = emptyList<Gene>()
    @JsonIgnore var motherGenes = emptyList<Gene>()
    @JsonIgnore var childGenes = emptyList<Gene>()

    override fun onDrawToScreen(engine: PulseEngine, surface: Surface2D)
    {
        // Get IDs of selected cars from roulette wheel (or properties)
        val rouletteWheel = engine.scene.getEntityOfType<RouletteWheel>(rouletteWheelId)
        val fatherCarId = rouletteWheel?.selectedId0 ?: fatherCarId
        val motherCarId = rouletteWheel?.selectedId1 ?: motherCarId

        // Find father and mother cars
        val fatherCar = engine.scene.getEntityOfType<Car>(fatherCarId)
        val motherCar = engine.scene.getEntityOfType<Car>(motherCarId)

        // Get genes from father and mother networks
        val fatherGenes = getFatherGenes(engine, fatherCar)
        val motherGenes = getMotherGenes(engine, motherCar)

        // Draw genes
        val totalHeight = height - (parentRowSpacing + childRowSpacing)
        val rowHeight = totalHeight / 3f
        val yStart = y - height / 2f
        drawGeneRow(engine, surface, x, yStart, width, rowHeight, fatherGenes)
        drawGeneRow(engine, surface, x, yStart + rowHeight + parentRowSpacing , width, rowHeight, motherGenes)
        drawGeneRow(engine, surface, x, yStart + rowHeight * 2 + parentRowSpacing + childRowSpacing, width, rowHeight, childGenes)
    }

    private fun drawGeneRow(engine: PulseEngine, surface: Surface2D, x: Float, y: Float, width: Float, height: Float, genes: List<Gene>)
    {
        // Draw template genes in editor
        val genesToDraw = if (genes.isEmpty() && engine.scene.state != RUNNING) templateGenes else genes

        val geneWidth = width / genesToDraw.size
        var xGene = x - width * 0.5f
        val yGene = y - height * 0.5f
        for (gene in genesToDraw)
        {
            val c = gene.color
            val a = 0.3f + 0.7f * min(abs(gene.value), 1f)
            surface.setDrawColor(c.red * a, c.green * a, c.blue * a, visibility)
            surface.drawTexture(BLANK, xGene, yGene, geneWidth - spacing, height, cornerRadius = cornerRadius)

            if (showText)
            {
                surface.setDrawColor(textColor)
                surface.drawText(
                    text = gene.value.format(),
                    x = xGene + 0.5f * (geneWidth - spacing),
                    y = yGene + 0.5f * height,
                    xOrigin = 0.5f,
                    yOrigin = 0.5f,
                    fontSize = textSize
                )
            }

            xGene += geneWidth
        }
    }

    private fun getFatherGenes(engine: PulseEngine, fatherCar: Car?): List<Gene>
    {
        if (fatherCar?.id == lastFatherCarId) return fatherGenes
        lastFatherCarId = fatherCar?.id
        fatherGenes = fatherCar?.network?.getWeights(engine)?.map { Gene(it, fatherCar.color) } ?: emptyList()
        return fatherGenes
    }

    private fun getMotherGenes(engine: PulseEngine, motherCar: Car?): List<Gene>
    {
        if (motherCar?.id == lastMotherCarId) return motherGenes
        lastMotherCarId = motherCar?.id
        motherGenes = motherCar?.network?.getWeights(engine)?.map { Gene(it, motherCar.color) } ?: emptyList()
        return motherGenes
    }

    private fun createChildGenes(fatherGenes: List<Gene>, motherGenes: List<Gene>): List<Gene>
    {
        if (fatherGenes.size != motherGenes.size)
            return emptyList() // Requires both parents to have the same amount of genes

        val totalGenes = fatherGenes.size
        val cutLength = (totalGenes * (cutLengthPercentage / 100f)).toInt()
        val cutPoint0 = (nextFloat() * (totalGenes - cutLength)).toInt()
        val cutPoint1 = cutPoint0 + cutLength

        // TODO: Swap mutation

        return (0 until totalGenes).map { i -> if (i > cutPoint0 && i < cutPoint1) fatherGenes[i] else motherGenes[i] }
    }

    override fun handleEvent(engine: PulseEngine, eventMessage: String)
    {
        when (eventMessage)
        {
            "RESET" -> childGenes = emptyList()
            "GENERATE" -> childGenes = createChildGenes(fatherGenes, motherGenes)
            "SUBMIT" ->
            {
                if (childGenes.isNotEmpty())
                {
                    val carPool = engine.scene.getEntityOfType<CarPool>(carPoolId) ?: return
                    val color = childGenes.random().color
                    val networkWeights = childGenes.mapToFloatArray { it.value }
                    carPool.addNextGenerationCar(engine, color, networkWeights)
                }
            }
        }
    }

    data class Gene(
        val value: Float,
        val color: Color
    )

    companion object
    {
        private val templateGenes = (0 until 25).map { Gene(nextFloat(), Color(1f, 1f, 1f)) }
    }
}