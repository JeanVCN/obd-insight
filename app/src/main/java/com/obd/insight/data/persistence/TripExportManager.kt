package com.obd.insight.data.persistence

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.obd.insight.domain.model.Trip
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

object TripExportManager {
    private val timestampFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

    fun createShareIntent(
        context: Context,
        trip: Trip,
        readings: List<SensorReadingEntity>
    ): Intent {
        val exportDirectory = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(exportDirectory, "obd_trip_${trip.id}.csv")
        file.writeText(buildCsv(trip, readings), Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Dados da viagem OBD ${trip.id}")
            putExtra(Intent.EXTRA_TEXT, "Dados brutos da viagem OBD ${trip.id}")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createPdfShareIntent(
        context: Context,
        trip: Trip,
        readings: List<SensorReadingEntity>
    ): Intent {
        val exportDirectory = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(exportDirectory, "obd_trip_${trip.id}_report.pdf")
        writePdf(file, trip, readings)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "Relatório da viagem OBD ${trip.id}")
            putExtra(Intent.EXTRA_TEXT, "Relatório processado da viagem OBD ${trip.id}")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun writePdf(file: File, trip: Trip, readings: List<SensorReadingEntity>) {
        val document = PdfDocument()
        var pageNumber = 0
        try {
            pageNumber++
            addPage(document, pageNumber) { canvas -> drawSummaryPage(canvas, trip, readings) }

            val chartReadings = chartPids(readings)
            chartReadings.chunked(2).forEach { pair ->
                pageNumber++
                addPage(document, pageNumber) { canvas ->
                    pair.forEachIndexed { index, pid ->
                        drawChart(canvas, readings.filter { it.pid == pid }, 48f, 92f + index * 350f, 499f, 292f)
                    }
                }
            }

            if (readings.isNotEmpty()) {
                pageNumber++
                addPage(document, pageNumber) { canvas -> drawSensorTable(canvas, readings) }
            }

            FileOutputStream(file).use { output -> document.writeTo(output) }
        } finally {
            document.close()
        }
    }

    private fun addPage(document: PdfDocument, pageNumber: Int, drawContent: (Canvas) -> Unit) {
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        page.canvas.drawColor(PDF_BG)
        drawContent(page.canvas)
        val footer = Paint(textPaint).apply {
            textSize = 9f
            color = PDF_MUTED
            typeface = Typeface.DEFAULT
        }
        page.canvas.drawText("OBD Insight  |  Página $pageNumber", 48f, PAGE_HEIGHT - 28f, footer)
        document.finishPage(page)
    }

    private fun drawSummaryPage(canvas: Canvas, trip: Trip, readings: List<SensorReadingEntity>) {
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 150f, Paint().apply { color = PDF_HEADER })
        canvas.drawText("OBD INSIGHT", 48f, 64f, titlePaint)
        canvas.drawText("Relatório da viagem", 48f, 105f, subtitlePaint)
        canvas.drawText(
            timestampFormatter.format(Instant.ofEpochMilli(trip.startedAt)),
            48f,
            132f,
            smallLightPaint
        )

        val duration = (trip.endedAt ?: readings.maxOfOrNull { it.recordedAt } ?: trip.startedAt) - trip.startedAt
        val cards = listOf(
            "Duração" to formatDuration(duration),
            "Leituras" to readings.size.toString(),
            "PIDs registrados" to readings.map { it.pid }.distinct().size.toString(),
            "Amostragem" to readings.groupBy { it.recordedAt }.size.toString()
        )
        cards.forEachIndexed { index, (label, value) ->
            val column = index % 2
            val row = index / 2
            drawMetricCard(canvas, 48f + column * 255f, 184f + row * 92f, label, value)
        }

        canvas.drawText("Principais métricas", 48f, 390f, sectionPaint)
        val metrics = listOf(0x0C to "RPM", 0x0D to "Velocidade", 0x05 to "Temperatura do líquido", 0x04 to "Carga do motor", 0x10 to "MAF", 0x42 to "Tensão do módulo")
        metrics.forEachIndexed { index, (pid, label) ->
            val values = readings.filter { it.pid == pid }.map { it.value }
            if (values.isNotEmpty()) {
                val y = 430f + index * 40f
                canvas.drawText(label, 58f, y, bodyPaint)
                canvas.drawText(
                    "mín ${number(values.minOrNull()!!)}  |  média ${number(values.average().toFloat())}  |  máx ${number(values.maxOrNull()!!)} ${readings.first { it.pid == pid }.unit}",
                    250f,
                    y,
                    bodyPaint
                )
            }
        }

        drawCallout(
            canvas,
            "Os gráficos e métricas deste relatório usam os valores processados. " +
                "O CSV complementar preserva as respostas hexadecimais brutas.",
            48f,
            700f,
            499f,
            58f
        )
    }

    private fun drawMetricCard(canvas: Canvas, left: Float, top: Float, label: String, value: String) {
        canvas.drawRoundRect(RectF(left, top, left + 235f, top + 72f), 10f, 10f, cardPaint)
        canvas.drawText(label, left + 14f, top + 25f, mutedBodyPaint)
        canvas.drawText(value, left + 14f, top + 55f, cardValuePaint)
    }

    private fun drawChart(canvas: Canvas, readings: List<SensorReadingEntity>, left: Float, top: Float, width: Float, height: Float) {
        if (readings.isEmpty()) return
        canvas.drawRoundRect(RectF(left, top, left + width, top + height), 10f, 10f, chartBackgroundPaint)
        val first = readings.first()
        canvas.drawText(ellipsize(first.label, width - 90f, sectionPaint), left + 18f, top + 28f, sectionPaint)
        canvas.drawText(first.unit, left + width - 42f, top + 28f, mutedBodyPaint)

        val plotLeft = left + 48f
        val plotTop = top + 54f
        val plotRight = left + width - 18f
        val plotBottom = top + height - 36f
        val values = readings.map { it.value }
        val accent = chartColor(first.pid)
        if (first.pid in BAR_PIDS) {
            drawBarChart(canvas, readings, plotLeft, plotTop, plotRight, plotBottom, values, accent)
            return
        }
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = max(0.001f, max - min)
        val padding = range * 0.1f
        val low = min - padding
        val high = max + padding

        val gridPaint = Paint().apply { color = PDF_GRID; strokeWidth = 1f }
        repeat(4) { index ->
            val y = plotTop + (plotBottom - plotTop) * index / 3f
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint)
            val value = high - (high - low) * index / 3f
            canvas.drawText(number(value), left + 4f, y + 4f, mutedBodyPaint)
        }
        canvas.drawText(number(low), plotLeft, plotBottom + 20f, mutedBodyPaint)
        canvas.drawText(number(high), plotRight - 40f, plotBottom + 20f, mutedBodyPaint)

        val path = Path()
        readings.forEachIndexed { index, reading ->
            val x = if (readings.size == 1) plotLeft else plotLeft + (plotRight - plotLeft) * index / (readings.size - 1)
            val y = plotBottom - ((reading.value - low) / (high - low)) * (plotBottom - plotTop)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val areaPath = Path(path).apply {
            lineTo(plotRight, plotBottom)
            lineTo(plotLeft, plotBottom)
            close()
        }
        canvas.drawPath(areaPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            alpha = 35
            style = Paint.Style.FILL
        })
        canvas.drawPath(path, Paint(linePaint).apply { color = accent })
    }

    private fun drawBarChart(
        canvas: Canvas,
        readings: List<SensorReadingEntity>,
        plotLeft: Float,
        plotTop: Float,
        plotRight: Float,
        plotBottom: Float,
        values: List<Float>,
        accent: Int
    ) {
        val min = 0f
        val max = max(100f, values.maxOrNull() ?: 100f)
        val gridPaint = Paint().apply { color = PDF_GRID; strokeWidth = 1f }
        repeat(4) { index ->
            val y = plotTop + (plotBottom - plotTop) * index / 3f
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint)
            canvas.drawText(number(max - max * index / 3f), plotLeft - 42f, y + 4f, mutedBodyPaint)
        }
        val slot = (plotRight - plotLeft) / values.size.coerceAtLeast(1)
        values.forEachIndexed { index, value ->
            val barLeft = plotLeft + index * slot + slot * .18f
            val barRight = plotLeft + index * slot + slot * .82f
            val barTop = plotBottom - (value / max) * (plotBottom - plotTop)
            canvas.drawRoundRect(
                RectF(barLeft, barTop, barRight, plotBottom),
                3f,
                3f,
                Paint(chartBarPaint).apply { color = accent }
            )
        }
        canvas.drawText("0", plotLeft, plotBottom + 20f, mutedBodyPaint)
    }

    private fun drawSensorTable(canvas: Canvas, readings: List<SensorReadingEntity>) {
        canvas.drawText("Resumo por sensor", 48f, 76f, pageTitlePaint)
        canvas.drawText("Valores calculados a partir de todas as amostras armazenadas", 48f, 100f, mutedBodyPaint)
        val summaries = readings.groupBy { it.pid }.values.sortedBy { it.first().pid }
        summaries.forEachIndexed { index, sensorReadings ->
            val column = index % 2
            val row = index / 2
            val x = 48f + column * 255f
            val y = 140f + row * 62f
            val values = sensorReadings.map { it.value }
            canvas.drawRoundRect(RectF(x, y, x + 235f, y + 48f), 6f, 6f, tablePaint)
            canvas.drawText(ellipsize(sensorReadings.first().label, 215f, bodyBoldPaint), x + 10f, y + 19f, bodyBoldPaint)
            canvas.drawText(
                "${number(values.minOrNull()!!)} / ${number(values.average().toFloat())} / ${number(values.maxOrNull()!!)} ${sensorReadings.first().unit}",
                x + 10f,
                y + 38f,
                mutedBodyPaint
            )
        }
    }

    private fun drawCallout(canvas: Canvas, text: String, left: Float, top: Float, width: Float, height: Float) {
        canvas.drawRoundRect(RectF(left, top, left + width, top + height), 8f, 8f, calloutPaint)
        drawWrappedText(canvas, text, left + 14f, top + 23f, width - 28f, bodyPaint)
    }

    private fun drawWrappedText(canvas: Canvas, text: String, left: Float, top: Float, width: Float, paint: Paint) {
        var line = ""
        var y = top
        text.split(" ").forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > width && line.isNotEmpty()) {
                canvas.drawText(line, left, y, paint)
                line = word
                y += paint.textSize + 4f
            } else {
                line = candidate
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line, left, y, paint)
    }

    private fun chartPids(readings: List<SensorReadingEntity>): List<Int> {
        val preferred = listOf(0x0C, 0x0D, 0x05, 0x04, 0x10, 0x11, 0x42, 0x2F)
        val available = readings.groupBy { it.pid }.filterValues { it.size > 1 }.keys
        return preferred.filter { it in available } + available.filter { it !in preferred }.sorted()
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = max(0L, durationMillis) / 1000L
        return "%02d:%02d:%02d".format(totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
    }

    private fun number(value: Float): String = String.format(Locale.US, "%.1f", value)

    private fun chartColor(pid: Int): Int = when (pid) {
        0x0C -> PDF_ORANGE
        0x0D -> PDF_BLUE
        0x05 -> PDF_AMBER
        0x04 -> PDF_GREEN
        0x10 -> PDF_PURPLE
        else -> PDF_TEAL
    }

    private fun ellipsize(text: String, width: Float, paint: Paint): String {
        if (paint.measureText(text) <= width) return text
        var result = text
        while (result.length > 1 && paint.measureText("$result…") > width) {
            result = result.dropLast(1)
        }
        return "$result…"
    }

    private fun buildCsv(trip: Trip, readings: List<SensorReadingEntity>): String = buildString {
        appendLine("trip_id,started_at,ended_at,recorded_at,elapsed_ms,pid,label,value,unit,raw_data")
        readings.forEach { reading ->
            val elapsed = reading.recordedAt - trip.startedAt
            appendLine(
                listOf(
                    trip.id,
                    timestampFormatter.format(Instant.ofEpochMilli(trip.startedAt)),
                    trip.endedAt?.let { timestampFormatter.format(Instant.ofEpochMilli(it)) }.orEmpty(),
                    timestampFormatter.format(Instant.ofEpochMilli(reading.recordedAt)),
                    elapsed,
                    "0x${reading.pid.toString(16).padStart(2, '0').uppercase()}",
                    reading.label,
                    String.format(Locale.US, "%.4f", reading.value),
                    reading.unit,
                    reading.rawData
                ).joinToString(",", transform = ::escapeCsv)
            )
        }
    }

    private fun escapeCsv(value: Any?): String {
        val text = value?.toString().orEmpty()
        return if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${text.replace("\"", "\"\"")}\""
        } else {
            text
        }
    }

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private val PDF_BG = Color.rgb(11, 18, 32)
    private val PDF_HEADER = Color.rgb(27, 54, 87)
    private val PDF_TEXT = Color.rgb(235, 240, 249)
    private val PDF_MUTED = Color.rgb(169, 187, 210)
    private val PDF_GRID = Color.rgb(54, 73, 100)
    private val PDF_ORANGE = Color.rgb(255, 154, 106)
    private val PDF_BLUE = Color.rgb(142, 200, 255)
    private val PDF_AMBER = Color.rgb(255, 183, 77)
    private val PDF_GREEN = Color.rgb(129, 199, 132)
    private val PDF_PURPLE = Color.rgb(186, 104, 200)
    private val PDF_TEAL = Color.rgb(103, 217, 208)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(textPaint).apply { color = Color.WHITE; textSize = 26f; typeface = Typeface.DEFAULT_BOLD }
    private val subtitlePaint = Paint(textPaint).apply { color = Color.rgb(205, 220, 240); textSize = 18f }
    private val smallLightPaint = Paint(textPaint).apply { color = Color.rgb(205, 220, 240); textSize = 10f }
    private val pageTitlePaint = Paint(textPaint).apply { color = PDF_TEXT; textSize = 22f; typeface = Typeface.DEFAULT_BOLD }
    private val sectionPaint = Paint(textPaint).apply { color = PDF_TEXT; textSize = 15f; typeface = Typeface.DEFAULT_BOLD }
    private val bodyPaint = Paint(textPaint).apply { color = PDF_TEXT; textSize = 11f }
    private val bodyBoldPaint = Paint(bodyPaint).apply { typeface = Typeface.DEFAULT_BOLD }
    private val mutedBodyPaint = Paint(textPaint).apply { color = PDF_MUTED; textSize = 10f }
    private val cardValuePaint = Paint(textPaint).apply { color = PDF_BLUE; textSize = 20f; typeface = Typeface.DEFAULT_BOLD }
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(25, 40, 64) }
    private val chartBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 32, 52) }
    private val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(25, 40, 64) }
    private val calloutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 65, 76) }
    private val chartBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PDF_BLUE }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(37, 112, 190)
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }
    private val BAR_PIDS = setOf(0x04, 0x06, 0x11, 0x2F, 0x45, 0x49, 0x4A, 0x4B, 0x4C, 0x5A)
}
