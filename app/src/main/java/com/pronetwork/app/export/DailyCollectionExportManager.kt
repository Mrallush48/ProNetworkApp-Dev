package com.pronetwork.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.pronetwork.app.data.DailyBuildingDetailedUi
import com.pronetwork.app.data.DailyClientCollection
import com.pronetwork.app.viewmodel.DailyCollectionUi
import com.pronetwork.data.DailySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider

class DailyCollectionExportManager(private val context: Context) {

    // ===================== EXCEL BUILDER =====================
    private fun buildExcelXml(
        date: String,
        ui: DailyCollectionUi,
        summary: DailySummary
    ): String {
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")

            // STYLES
            appendLine("""<Styles>""")
            appendLine("""<Style ss:ID="Default"><Font ss:Size="10"/></Style>""")
            appendLine("""<Style ss:ID="Title"><Font ss:Size="16" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#673AB7" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center"/></Style>""")
            appendLine("""<Style ss:ID="Section"><Font ss:Size="12" ss:Bold="1" ss:Color="#4527A0"/><Interior ss:Color="#EDE7F6" ss:Pattern="Solid"/></Style>""")
            appendLine("""<Style ss:ID="Header"><Font ss:Size="9" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#7E57C2" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center"/></Style>""")
            appendLine("""<Style ss:ID="Cell"><Font ss:Size="9"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
            appendLine("""<Style ss:ID="CellAlt"><Font ss:Size="9"/><Interior ss:Color="#F5F5F5" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
            appendLine("""<Style ss:ID="Currency"><NumberFormat ss:Format="#,##0.00"/><Font ss:Size="9"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
            appendLine("""<Style ss:ID="CurrencyAlt"><NumberFormat ss:Format="#,##0.00"/><Font ss:Size="9"/><Interior ss:Color="#F5F5F5" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
            appendLine("""<Style ss:ID="Total"><Font ss:Size="10" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#9575CD" ss:Pattern="Solid"/><NumberFormat ss:Format="#,##0.00"/></Style>""")
            appendLine("""<Style ss:ID="Good"><Font ss:Size="9" ss:Bold="1" ss:Color="#2E7D32"/></Style>""")
            appendLine("""<Style ss:ID="Warn"><Font ss:Size="9" ss:Bold="1" ss:Color="#F57F17"/></Style>""")
            appendLine("""<Style ss:ID="Bad"><Font ss:Size="9" ss:Bold="1" ss:Color="#C62828"/></Style>""")
            appendLine("""<Style ss:ID="TrendStable"><Font ss:Size="9" ss:Bold="1" ss:Color="#1565C0"/></Style>""")
            // Building group colors (dark/light pairs)
            val bgColors = listOf(
                "#E3F2FD" to "#BBDEFB", "#E8F5E9" to "#C8E6C9",
                "#F3E5F5" to "#E1BEE7", "#FFF3E0" to "#FFE0B2",
                "#E0F7FA" to "#B2EBF2", "#FCE4EC" to "#F8BBD0",
                "#F1F8E9" to "#DCEDC8", "#EDE7F6" to "#D1C4E9"
            )
            bgColors.forEachIndexed { i, (light, dark) ->
                appendLine("""<Style ss:ID="BG${i}L"><Font ss:Size="9"/><Interior ss:Color="$light" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
                appendLine("""<Style ss:ID="BG${i}D"><Font ss:Size="9"/><Interior ss:Color="$dark" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
                appendLine("""<Style ss:ID="BG${i}LC"><NumberFormat ss:Format="#,##0.00"/><Font ss:Size="9"/><Interior ss:Color="$light" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
                appendLine("""<Style ss:ID="BG${i}DC"><NumberFormat ss:Format="#,##0.00"/><Font ss:Size="9"/><Interior ss:Color="$dark" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
            }
            appendLine("""</Styles>""")

            // SHEET 1: Dashboard
            appendLine("""<Worksheet ss:Name="Dashboard">""")
            appendLine("""<Table>""")
            appendLine("""<Column ss:Width="160"/><Column ss:Width="100"/><Column ss:Width="100"/><Column ss:Width="100"/>""")

            appendLine("""<Row><Cell ss:StyleID="Title" ss:MergeAcross="3"><Data ss:Type="String">Daily Collection Report - $date</Data></Cell></Row>""")
            appendLine("""<Row/>""")

            // KPIs
            appendLine("""<Row><Cell ss:StyleID="Section" ss:MergeAcross="3"><Data ss:Type="String">Summary</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Metric</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Value</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Total Collected</Data></Cell><Cell ss:StyleID="Currency"><Data ss:Type="Number">${ui.totalAmount}</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Total Expected</Data></Cell><Cell ss:StyleID="Currency"><Data ss:Type="Number">${ui.totalExpected}</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Collection Rate</Data></Cell><Cell ss:StyleID="Cell"><Data ss:Type="String">${"%.1f".format(ui.overallCollectionRate)}%</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Clients Count</Data></Cell><Cell ss:StyleID="Cell"><Data ss:Type="Number">${summary.totalClients}</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Transactions Count</Data></Cell><Cell ss:StyleID="Cell"><Data ss:Type="Number">${summary.totalTransactions}</Data></Cell></Row>""")
            if (ui.topBuilding != null) {
                appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Top Building</Data></Cell><Cell ss:StyleID="Good"><Data ss:Type="String">${ui.topBuilding}</Data></Cell></Row>""")
            }
            if (ui.lowBuilding != null && ui.buildings.size > 1) {
                appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Lowest Building</Data></Cell><Cell ss:StyleID="Bad"><Data ss:Type="String">${ui.lowBuilding}</Data></Cell></Row>""")
            }

            appendLine("""<Row/>""")

            // Building Summary Table
            appendLine("""<Row><Cell ss:StyleID="Section" ss:MergeAcross="3"><Data ss:Type="String">Collection by Building</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Building</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Clients</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Collected</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Rate</Data></Cell></Row>""")

            ui.buildings.forEachIndexed { idx, b ->
                val style = if (idx % 2 == 0) "Cell" else "CellAlt"
                val curStyle = if (idx % 2 == 0) "Currency" else "CurrencyAlt"
                val rateStyle = when {
                    b.collectionRate >= 80 -> "Good"
                    b.collectionRate >= 50 -> "Warn"
                    else -> "Bad"
                }
                appendLine("""<Row><Cell ss:StyleID="$style"><Data ss:Type="String">${b.buildingName}</Data></Cell><Cell ss:StyleID="$style"><Data ss:Type="Number">${b.clientsCount}</Data></Cell><Cell ss:StyleID="$curStyle"><Data ss:Type="Number">${b.totalAmount}</Data></Cell><Cell ss:StyleID="$rateStyle"><Data ss:Type="String">${"%.1f".format(b.collectionRate)}%</Data></Cell></Row>""")
            }

            // Total row
            appendLine("""<Row><Cell ss:StyleID="Total"><Data ss:Type="String">TOTAL</Data></Cell><Cell ss:StyleID="Total"><Data ss:Type="Number">${ui.totalClientsCount}</Data></Cell><Cell ss:StyleID="Total"><Data ss:Type="Number">${ui.totalAmount}</Data></Cell><Cell ss:StyleID="Total"><Data ss:Type="String">${"%.1f".format(ui.overallCollectionRate)}%</Data></Cell></Row>""")

            appendLine("""</Table>""")
            appendLine("""</Worksheet>""")

            // SHEET 2: Client Details
            appendLine("""<Worksheet ss:Name="Client Details">""")
            appendLine("""<Table>""")
            appendLine("""<Column ss:Width="120"/><Column ss:Width="80"/><Column ss:Width="100"/><Column ss:Width="50"/><Column ss:Width="70"/><Column ss:Width="80"/><Column ss:Width="80"/><Column ss:Width="60"/><Column ss:Width="70"/><Column ss:Width="80"/><Column ss:Width="150"/>""")

            appendLine("""<Row><Cell ss:StyleID="Title" ss:MergeAcross="10"><Data ss:Type="String">Client Details - $date</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Client</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Sub#</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Building</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Room</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Package</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Monthly</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Paid</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Rate</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Time</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Notes</Data></Cell></Row>""")

            ui.buildings.forEachIndexed { bIdx, b ->
                val colorIdx = bIdx % 8
                b.clients.forEachIndexed { cIdx, c ->
                    val cellStyle = if (cIdx % 2 == 0) "BG${colorIdx}L" else "BG${colorIdx}D"
                    val curStyle = if (cIdx % 2 == 0) "BG${colorIdx}LC" else "BG${colorIdx}DC"
                    val paidRate = if (c.monthlyAmount > 0) (c.paidAmount / c.monthlyAmount) * 100 else 0.0
                    val rateStyle = when {
                        paidRate >= 100 -> "Good"
                        paidRate >= 50 -> "Warn"
                        else -> "Bad"
                    }
                    val statusIcon = when (c.paymentStatus) {
                        "PAID" -> "\u2705"
                        "SETTLED" -> "\uD83D\uDD35"
                        "PARTIAL" -> "\u26A0\uFE0F"
                        else -> "\u274C"
                    }
                    val statusLabel = when (c.paymentStatus) {
                        "PAID" -> "Paid"
                        "SETTLED" -> "Settled"
                        "PARTIAL" -> "Partial"
                        else -> "Unpaid"
                    }
                    val statusStyle = when (c.paymentStatus) {
                        "PAID" -> "Good"
                        "SETTLED" -> "TrendStable"
                        "PARTIAL" -> "Warn"
                        else -> "Bad"
                    }
                    appendLine("""<Row><Cell ss:StyleID="$cellStyle"><Data ss:Type="String">${c.clientName}</Data></Cell><Cell ss:StyleID="$cellStyle"><Data ss:Type="String">${c.subscriptionNumber}</Data></Cell><Cell ss:StyleID="$cellStyle"><Data ss:Type="String">${b.buildingName}</Data></Cell><Cell ss:StyleID="$cellStyle"><Data ss:Type="String">${c.roomNumber ?: "-"}</Data></Cell><Cell ss:StyleID="$cellStyle"><Data ss:Type="String">${c.packageType}</Data></Cell><Cell ss:StyleID="$curStyle"><Data ss:Type="Number">${c.monthlyAmount}</Data></Cell><Cell ss:StyleID="$curStyle"><Data ss:Type="Number">${c.paidAmount}</Data></Cell><Cell ss:StyleID="$rateStyle"><Data ss:Type="String">${"%.0f".format(paidRate)}%</Data></Cell><Cell ss:StyleID="$statusStyle"><Data ss:Type="String">$statusIcon $statusLabel</Data></Cell><Cell ss:StyleID="$cellStyle"><Data ss:Type="String">${c.transactionTime}</Data></Cell><Cell ss:StyleID="$cellStyle"><Data ss:Type="String">${c.notes}</Data></Cell></Row>""")
                }
            }

            // Total
            appendLine("""<Row><Cell ss:StyleID="Total"><Data ss:Type="String">TOTAL</Data></Cell><Cell ss:StyleID="Total"/><Cell ss:StyleID="Total"/><Cell ss:StyleID="Total"/><Cell ss:StyleID="Total"/><Cell ss:StyleID="Total"><Data ss:Type="Number">${ui.totalExpected}</Data></Cell><Cell ss:StyleID="Total"><Data ss:Type="Number">${ui.totalAmount}</Data></Cell><Cell ss:StyleID="Total"><Data ss:Type="String">${"%.1f".format(ui.overallCollectionRate)}%</Data></Cell><Cell ss:StyleID="Total"/><Cell ss:StyleID="Total"/><Cell ss:StyleID="Total"/></Row>""")

            appendLine("""</Table>""")
            appendLine("""</Worksheet>""")

            // SHEET 3: All Transactions
            appendLine("""<Worksheet ss:Name="Transactions">""")
            appendLine("""<Table>""")
            appendLine("""<Column ss:Width="120"/><Column ss:Width="80"/><Column ss:Width="100"/><Column ss:Width="100"/><Column ss:Width="80"/><Column ss:Width="80"/><Column ss:Width="150"/>""")

            appendLine("""<Row><Cell ss:StyleID="Title" ss:MergeAcross="6"><Data ss:Type="String">Transaction Audit Trail - $date</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Client</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Sub#</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Building</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Time</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Amount</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Type</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Notes</Data></Cell></Row>""")

            ui.buildings.forEachIndexed { bIdx, b ->
                val colorIdx = bIdx % 8
                var txInBuilding = 0
                b.clients.forEach { c ->
                    c.transactions.forEach { tx ->
                        val style = if (txInBuilding % 2 == 0) "BG${colorIdx}L" else "BG${colorIdx}D"
                        val curStyle = if (txInBuilding % 2 == 0) "BG${colorIdx}LC" else "BG${colorIdx}DC"
                        val typeStyle = if (tx.type == "Refund") "Bad" else "Good"
                        appendLine("""<Row><Cell ss:StyleID="$style"><Data ss:Type="String">${c.clientName}</Data></Cell><Cell ss:StyleID="$style"><Data ss:Type="String">${c.subscriptionNumber}</Data></Cell><Cell ss:StyleID="$style"><Data ss:Type="String">${b.buildingName}</Data></Cell><Cell ss:StyleID="$style"><Data ss:Type="String">${tx.time}</Data></Cell><Cell ss:StyleID="$curStyle"><Data ss:Type="Number">${tx.amount}</Data></Cell><Cell ss:StyleID="$typeStyle"><Data ss:Type="String">${tx.type}</Data></Cell><Cell ss:StyleID="$style"><Data ss:Type="String">${tx.notes}</Data></Cell></Row>""")
                        txInBuilding++
                    }
                }
            }

            appendLine("""</Table>""")
            appendLine("""</Worksheet>""")

            appendLine("""</Workbook>""")

        }
    }
    // ===================== PDF BUILDER =====================
    private fun buildPdf(
        date: String,
        ui: DailyCollectionUi,
        summary: DailySummary
    ): ByteArray {
        val pdfDocument = android.graphics.pdf.PdfDocument()

        val titlePaint = android.graphics.Paint().apply {
            textSize = 18f; color = android.graphics.Color.parseColor("#673AB7"); isFakeBoldText = true
        }
        val sectionPaint = android.graphics.Paint().apply {
            textSize = 12f; color = android.graphics.Color.parseColor("#4527A0"); isFakeBoldText = true
        }
        val headerPaint = android.graphics.Paint().apply {
            textSize = 8f; color = android.graphics.Color.WHITE; isFakeBoldText = true
        }
        val cellPaint = android.graphics.Paint().apply {
            textSize = 7f; color = android.graphics.Color.BLACK
        }
        val fillPaint = android.graphics.Paint()

        val pageWidth = 842
        val pageHeight = 595
        val margin = 20f
        var yPos = 45f
        var pageNum = 1

        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun checkNewPage() {
            if (yPos > pageHeight - 40) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 30f
            }
        }

        // Title
        canvas.drawText("Pro Network - Daily Collection Report", margin, yPos, titlePaint)
        yPos += 18f
        cellPaint.textSize = 9f
        canvas.drawText("Date: $date | Clients: ${summary.totalClients} | Transactions: ${summary.totalTransactions}", margin, yPos, cellPaint)
        yPos += 20f

        // KPIs
        canvas.drawText("Summary", margin, yPos, sectionPaint)
        yPos += 14f
        cellPaint.textSize = 8f

        val rateColor = when {
            ui.overallCollectionRate >= 80 -> android.graphics.Color.parseColor("#2E7D32")
            ui.overallCollectionRate >= 50 -> android.graphics.Color.parseColor("#F57F17")
            else -> android.graphics.Color.parseColor("#C62828")
        }
        canvas.drawText("Collected: ${"%.2f".format(ui.totalAmount)} SAR | Expected: ${"%.2f".format(ui.totalExpected)} SAR", margin, yPos, cellPaint)
        yPos += 12f
        val ratePaint = android.graphics.Paint().apply {
            textSize = 9f; color = rateColor; isFakeBoldText = true
        }
        canvas.drawText("Collection Rate: ${"%.1f".format(ui.overallCollectionRate)}%", margin, yPos, ratePaint)
        yPos += 18f

        // Building Summary Table
        checkNewPage()
        canvas.drawText("Collection by Building", margin, yPos, sectionPaint)
        yPos += 14f

        fillPaint.color = android.graphics.Color.parseColor("#7E57C2")
        canvas.drawRect(margin, yPos - 10f, pageWidth - margin, yPos + 4f, fillPaint)
        val bHeaders = listOf("Building", "Clients", "Collected", "Expected", "Rate")
        val bW = listOf(160f, 70f, 100f, 100f, 80f)
        var xPos = margin
        bHeaders.forEachIndexed { i, h ->
            canvas.drawText(h, xPos + 2f, yPos, headerPaint)
            xPos += bW[i]
        }
        yPos += 14f

        ui.buildings.forEachIndexed { idx, b ->
            checkNewPage()
            if (idx % 2 == 1) {
                fillPaint.color = android.graphics.Color.parseColor("#F5F5F5")
                canvas.drawRect(margin, yPos - 9f, pageWidth - margin, yPos + 3f, fillPaint)
            }
            xPos = margin
            listOf(
                b.buildingName,
                b.clientsCount.toString(),
                "%.2f".format(b.totalAmount),
                "%.2f".format(b.expectedAmount),
                "%.1f".format(b.collectionRate) + "%"
            ).forEachIndexed { i, d ->
                canvas.drawText(d, xPos + 2f, yPos, cellPaint)
                xPos += bW[i]
            }
            yPos += 12f
        }
        yPos += 10f

        // Client Details Table
        checkNewPage()
        canvas.drawText("Client Details", margin, yPos, sectionPaint)
        yPos += 14f

        fillPaint.color = android.graphics.Color.parseColor("#7E57C2")
        canvas.drawRect(margin, yPos - 10f, pageWidth - margin, yPos + 4f, fillPaint)
        val cHeaders = listOf("Client", "Sub#", "Building", "Room", "Monthly", "Paid", "Rate", "Status", "Time", "Notes")
        val cW = listOf(90f, 60f, 40f, 75f, 60f, 60f, 45f, 45f, 60f, 167f)
        xPos = margin
        cHeaders.forEachIndexed { i, h ->
            canvas.drawText(h, xPos + 2f, yPos, headerPaint)
            xPos += cW[i]
        }
        yPos += 14f

        var rowNum = 0
        ui.buildings.forEach { b ->
            b.clients.forEach { c ->
                checkNewPage()
                if (rowNum % 2 == 1) {
                    fillPaint.color = android.graphics.Color.parseColor("#F5F5F5")
                    canvas.drawRect(margin, yPos - 9f, pageWidth - margin, yPos + 3f, fillPaint)
                }
                val paidRate = if (c.monthlyAmount > 0) (c.paidAmount / c.monthlyAmount) * 100 else 0.0
                xPos = margin
                val statusText = when (c.paymentStatus) {
                    "PAID" -> "Paid"
                    "SETTLED" -> "Settled"
                    "PARTIAL" -> "Partial"
                    else -> "Unpaid"
                }
                val rowVals = listOf(
                    c.clientName,
                    c.subscriptionNumber,
                    b.buildingName,
                    c.roomNumber ?: "-",
                    "%.2f".format(c.monthlyAmount),
                    "%.2f".format(c.paidAmount),
                    "%.0f".format(paidRate) + "%",
                    statusText,
                    c.transactionTime,
                    c.notes
                )
                rowVals.forEachIndexed { i, d ->
                    val maxLen = if (i == cHeaders.lastIndex) 30 else 14
                    val text = if (d.length > maxLen) d.take(maxLen - 2) + ".." else d
                    canvas.drawText(text, xPos + 2f, yPos, cellPaint)
                    xPos += cW[i]
                }
                yPos += 12f
                rowNum++
            }
        }

        // Total row
        checkNewPage()
        fillPaint.color = android.graphics.Color.parseColor("#9575CD")
        canvas.drawRect(margin, yPos - 9f, pageWidth - margin, yPos + 3f, fillPaint)
        val totalPaint = android.graphics.Paint().apply {
            textSize = 8f; color = android.graphics.Color.WHITE; isFakeBoldText = true
        }
        xPos = margin
        canvas.drawText("TOTAL", xPos + 2f, yPos, totalPaint)
        xPos += cW[0] + cW[1] + cW[2] + cW[3]
        canvas.drawText("%.2f".format(ui.totalExpected), xPos + 2f, yPos, totalPaint)
        xPos += cW[4]
        canvas.drawText("%.2f".format(ui.totalAmount), xPos + 2f, yPos, totalPaint)
        xPos += cW[5]
        canvas.drawText("%.1f".format(ui.overallCollectionRate) + "%", xPos + 2f, yPos, totalPaint)

        pdfDocument.finishPage(page)
        val outputStream = java.io.ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        return outputStream.toByteArray()
    }

    // ===================== EXPORT FUNCTIONS =====================
    suspend fun exportExcelToDownloads(date: String, ui: DailyCollectionUi, summary: DailySummary) {
        withContext(Dispatchers.IO) {
            val xml = buildExcelXml(date, ui, summary)
            val fileName = "daily_collection_${date}.xls"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.ms-excel")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(xml.toByteArray(Charsets.UTF_8))
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        }
    }

    suspend fun exportPdfToDownloads(date: String, ui: DailyCollectionUi, summary: DailySummary) {
        withContext(Dispatchers.IO) {
            val pdfBytes = buildPdf(date, ui, summary)
            val fileName = "daily_collection_${date}.pdf"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(pdfBytes)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        }
    }

    suspend fun sharePdf(date: String, ui: DailyCollectionUi, summary: DailySummary) {
        withContext(Dispatchers.IO) {
            val pdfBytes = buildPdf(date, ui, summary)
            val fileName = "daily_collection_${date}.pdf"
            val file = java.io.File(context.cacheDir, fileName)
            file.writeBytes(pdfBytes)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            withContext(Dispatchers.Main) {
                try {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Share Daily Report").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    suspend fun shareExcel(date: String, ui: DailyCollectionUi, summary: DailySummary) {
        withContext(Dispatchers.IO) {
            val xml = buildExcelXml(date, ui, summary)
            val fileName = "daily_collection_${date}.xls"
            val file = java.io.File(context.cacheDir, fileName)
            file.writeText(xml, Charsets.UTF_8)

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            withContext(Dispatchers.Main) {
                try {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.ms-excel"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Share Daily Collection").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}