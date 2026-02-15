package com.pronetwork.app.export

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.pronetwork.app.R
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClientsExportManager(
    private val context: Context
) {

    fun exportExcelToDownloads(
        clients: List<Client>,
        buildings: List<Building>
    ) {
        val htmlContent = exportClientsToExcelHTML(clients, buildings)
        saveFileToDownloads(
            contentResolver = context.contentResolver,
            content = htmlContent,
            fileName = "clients_export.xls",
            mimeType = "application/vnd.ms-excel"
        )
    }

    fun exportPdfToDownloads(
        clients: List<Client>,
        buildings: List<Building>
    ) {
        val pdfBytes = generateClientsPDF(clients, buildings)
        savePDFToDownloads(
            contentResolver = context.contentResolver,
            pdfBytes = pdfBytes,
            fileName = "clients_report.pdf"
        )
    }

    fun shareExcel(
        clients: List<Client>,
        buildings: List<Building>
    ) {
        val htmlContent = exportClientsToExcelHTML(clients, buildings)
        shareFile(
            content = htmlContent,
            fileName = "clients_export.xls",
            mimeType = "application/vnd.ms-excel"
        )
    }

    fun sharePdf(
        clients: List<Client>,
        buildings: List<Building>
    ) {
        val pdfBytes = generateClientsPDF(clients, buildings)
        sharePDFFile(
            pdfBytes = pdfBytes,
            fileName = "clients_report.pdf"
        )
    }

    private fun shareFile(content: String, fileName: String, mimeType: String) {
        try {
            val file = File(context.cacheDir, fileName)
            file.writeText(content)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    context.getString(R.string.share_clients)
                )
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.export_error, e.message ?: "Unknown"),
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }

    private fun sharePDFFile(pdfBytes: ByteArray, fileName: String) {
        try {
            val file = File(context.cacheDir, fileName)
            file.writeBytes(pdfBytes)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    context.getString(R.string.share_clients)
                )
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.export_error, e.message ?: "Unknown"),
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }

    private fun saveFileToDownloads(
        contentResolver: ContentResolver,
        content: String,
        fileName: String,
        mimeType: String
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val uri: Uri? =
                    contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(it, contentValues, null, null)

                    Toast.makeText(
                        context,
                        context.getString(R.string.export_saved_to_downloads, fileName),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.writeText(content)
                Toast.makeText(
                    context,
                    context.getString(R.string.export_saved_to_downloads, file.name),
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.export_error, e.message ?: "Unknown"),
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }

    private fun savePDFToDownloads(
        contentResolver: ContentResolver,
        pdfBytes: ByteArray,
        fileName: String
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val uri: Uri? =
                    contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(pdfBytes)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(it, contentValues, null, null)

                    Toast.makeText(
                        context,
                        context.getString(R.string.export_saved_to_downloads, fileName),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.writeBytes(pdfBytes)
                Toast.makeText(
                    context,
                    context.getString(R.string.export_saved_to_downloads, file.name),
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.export_error, e.message ?: "Unknown"),
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }

    private fun generateClientsPDF(
        clients: List<Client>,
        buildings: List<Building>
    ): ByteArray {
        val pdfDocument = PdfDocument()
        val paint = android.graphics.Paint()
        val titlePaint = android.graphics.Paint().apply {
            textSize = 24f
            color = android.graphics.Color.parseColor("#673AB7")
            isFakeBoldText = true
        }
        val headerPaint = android.graphics.Paint().apply {
            textSize = 10f
            color = android.graphics.Color.WHITE
            isFakeBoldText = true
        }
        val cellPaint = android.graphics.Paint().apply {
            textSize = 9f
            color = android.graphics.Color.BLACK
        }

        val pageWidth = 842f
        val pageHeight = 595f
        val margin = 20f
        var yPosition = 50f

        val pageInfo = PdfDocument.PageInfo.Builder(
            pageWidth.toInt(),
            pageHeight.toInt(),
            1
        ).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawText(
            "\uD83D\uDCCA Pro Network Spot - Clients Report",
            margin,
            yPosition,
            titlePaint
        )
        yPosition += 30f

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        cellPaint.textSize = 10f
        canvas.drawText("Generated: ${dateFormat.format(Date())}", margin, yPosition, cellPaint)
        yPosition += 25f

        paint.color = android.graphics.Color.parseColor("#673AB7")
        canvas.drawRect(margin, yPosition - 18f, pageWidth - margin, yPosition + 7f, paint)

        val headers = listOf(
            "Name", "Sub#", "Phone", "Package", "Price",
            "Building", "Room", "Month", "Day", "Address", "Notes"
        )
        val colWidths = listOf(65f, 48f, 60f, 50f, 42f, 60f, 40f, 45f, 30f, 75f, 90f)
        var xPos = margin
        headers.forEachIndexed { i, header ->
            canvas.drawText(header, xPos + 3f, yPosition, headerPaint)
            xPos += colWidths[i]
        }
        yPosition += 15f

        cellPaint.textSize = 8f
        clients.forEachIndexed { index, client ->
            if (yPosition > pageHeight - 50f) {
                canvas.drawText(
                    "... (${clients.size - index} more)",
                    margin, yPosition, cellPaint
                )
                return@forEachIndexed
            }

            val buildingName = buildings.find { it.id == client.buildingId }?.name ?: "N/A"

            if (index % 2 == 0) {
                paint.color = android.graphics.Color.parseColor("#F5F5F5")
                canvas.drawRect(margin, yPosition - 12f, pageWidth - margin, yPosition + 5f, paint)
            }

            xPos = margin
            val rowData = listOf(
                client.name, client.subscriptionNumber, client.phone,
                client.packageType, "${client.price}", buildingName,
                client.roomNumber ?: "-", client.startMonth, "${client.startDay}",
                client.address, client.notes
            )

            rowData.forEachIndexed { i, data ->
                val truncated = if (data.length > 15) data.take(12) + "..." else data
                canvas.drawText(truncated, xPos + 3f, yPosition, cellPaint)
                xPos += colWidths[i]
            }
            yPosition += 15f
        }

        yPosition += 10f
        paint.color = android.graphics.Color.parseColor("#9575CD")
        canvas.drawRect(margin, yPosition - 12f, pageWidth - margin, yPosition + 5f, paint)
        headerPaint.textSize = 10f
        canvas.drawText(
            "Total: ${clients.sumOf { it.price }} SAR",
            margin + 5f, yPosition, headerPaint
        )

        pdfDocument.finishPage(page)

        val outputStream = java.io.ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        return outputStream.toByteArray()
    }

    private fun exportClientsToExcelHTML(
        clients: List<Client>,
        buildings: List<Building>
    ): String {
        val currentDate = SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
        ).format(Date())
        val total = clients.sumOf { it.price }

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine(
                """<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">"""
            )

            appendLine("""<Styles>""")
            appendLine(
                """<Style ss:ID="Title"><Font ss:Bold="1" ss:Size="18" ss:Color="#673AB7"/><Alignment ss:Horizontal="Center"/></Style>"""
            )
            appendLine(
                """<Style ss:ID="Subtitle"><Font ss:Italic="1" ss:Size="10" ss:Color="#666666"/><Alignment ss:Horizontal="Center"/></Style>"""
            )
            appendLine(
                """<Style ss:ID="Header"><Font ss:Bold="1" ss:Size="11" ss:Color="#FFFFFF"/><Interior ss:Color="#673AB7" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center" ss:Vertical="Center"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#000000"/><Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1"/></Borders></Style>"""
            )
            appendLine(
                """<Style ss:ID="EvenRow"><Interior ss:Color="#EDE7F6" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/></Borders></Style>"""
            )
            appendLine(
                """<Style ss:ID="OddRow"><Interior ss:Color="#FFFFFF" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/></Borders></Style>"""
            )
            appendLine(
                """<Style ss:ID="CurrencyEven"><NumberFormat ss:Format="#,##0.00"/><Font ss:Color="#673AB7"/><Interior ss:Color="#EDE7F6" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/></Borders></Style>"""
            )
            appendLine(
                """<Style ss:ID="CurrencyOdd"><NumberFormat ss:Format="#,##0.00"/><Font ss:Color="#673AB7"/><Interior ss:Color="#FFFFFF" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/></Borders></Style>"""
            )
            appendLine(
                """<Style ss:ID="Total"><Font ss:Bold="1" ss:Size="12" ss:Color="#FFFFFF"/><Interior ss:Color="#9575CD" ss:Pattern="Solid"/><NumberFormat ss:Format="#,##0.00"/><Alignment ss:Horizontal="Right"/></Style>"""
            )
            appendLine(
                """<Style ss:ID="TotalLabel"><Font ss:Bold="1" ss:Size="12" ss:Color="#FFFFFF"/><Interior ss:Color="#9575CD" ss:Pattern="Solid"/><Alignment ss:Horizontal="Right"/></Style>"""
            )
            appendLine("""</Styles>""")

            appendLine("""<Worksheet ss:Name="Clients">""")
            appendLine("""<Table ss:DefaultColumnWidth="100">""")

            appendLine("""<Column ss:Width="150"/>""")  // Name
            appendLine("""<Column ss:Width="120"/>""")  // Subscription
            appendLine("""<Column ss:Width="100"/>""")  // Phone
            appendLine("""<Column ss:Width="80"/>""")   // Package
            appendLine("""<Column ss:Width="80"/>""")   // Price
            appendLine("""<Column ss:Width="120"/>""")  // Building
            appendLine("""<Column ss:Width="80"/>""")   // Room
            appendLine("""<Column ss:Width="90"/>""")   // Start Month
            appendLine("""<Column ss:Width="70"/>""")   // Day
            appendLine("""<Column ss:Width="150"/>""")  // Address
            appendLine("""<Column ss:Width="200"/>""")  // Notes


            appendLine("""<Row ss:Height="30">""")
            appendLine(
                """<Cell ss:StyleID="Title" ss:MergeAcross="10"><Data ss:Type="String">📊 Pro Network Spot - Clients Report</Data></Cell>"""
            )
            appendLine("""</Row>""")

            appendLine("""<Row ss:Height="18">""")
            appendLine(
                """<Cell ss:StyleID="Subtitle" ss:MergeAcross="10"><Data ss:Type="String">Generated: $currentDate | Total: ${clients.size} clients</Data></Cell>"""
            )
            appendLine("""</Row>""")

            appendLine("""<Row ss:Height="10"/>""")

            appendLine("""<Row ss:Height="25">""")
            listOf(
                "Name", "Subscription #", "Phone", "Package", "Price (SAR)",
                "Building", "Room", "Start Month", "Day", "Address", "Notes"
            ).forEach {
                appendLine(
                    """<Cell ss:StyleID="Header"><Data ss:Type="String">$it</Data></Cell>"""
                )
            }
            appendLine("""</Row>""")

            clients.forEachIndexed { index, client ->
                val buildingName = buildings.find { it.id == client.buildingId }?.name ?: "N/A"
                val rowStyle = if (index % 2 == 0) "EvenRow" else "OddRow"
                val currencyStyle = if (index % 2 == 0) "CurrencyEven" else "CurrencyOdd"

                appendLine("""<Row ss:Height="20">""")
                appendLine(
                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.name}</Data></Cell>"""
                )
                appendLine(
                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.subscriptionNumber}</Data></Cell>"""
                )
                appendLine(
                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.phone}</Data></Cell>"""
                )
                appendLine(
                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.packageType}</Data></Cell>"""
                )
                appendLine(
                    """<Cell ss:StyleID="$currencyStyle"><Data ss:Type="Number">${client.price}</Data></Cell>"""
                )
                appendLine(
                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">$buildingName</Data></Cell>"""
                )
                appendLine(
                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.roomNumber ?: "-"}</Data></Cell>"""
                )
                appendLine(
                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.startMonth}</Data></Cell>"""
                )
                appendLine(
                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="Number">${client.startDay}</Data></Cell>"""
                )
                appendLine(
                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.address}</Data></Cell>"""
                )
                appendLine(
                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.notes}</Data></Cell>"""
                )
                appendLine("""</Row>""")
            }

            appendLine("""<Row ss:Height="5"/>""")

            appendLine("""<Row ss:Height="25">""")
            appendLine(
                """<Cell ss:StyleID="TotalLabel" ss:MergeAcross="3"><Data ss:Type="String">Total Amount:</Data></Cell>"""
            )
            appendLine(
                """<Cell ss:StyleID="Total"><Data ss:Type="Number">$total</Data></Cell>"""
            )
            appendLine(
                """<Cell ss:StyleID="TotalLabel" ss:MergeAcross="5"><Data ss:Type="String">SAR</Data></Cell>"""
            )

            appendLine("""</Row>""")

            appendLine("""</Table>""")
            appendLine("""</Worksheet>""")
            appendLine("""</Workbook>""")
        }
    }
}
