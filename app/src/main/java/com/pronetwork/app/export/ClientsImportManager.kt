package com.pronetwork.app.export

import android.content.Context
import android.net.Uri
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClientsImportManager(
    private val context: Context,
    private val buildingRepository: com.pronetwork.app.repository.BuildingRepository,
    private val db: com.pronetwork.app.data.ClientDatabase
) {

    data class ImportResult(
        val success: Int,
        val skipped: Int,
        val newBuildings: Int,
        val errors: List<String>
    )

    suspend fun importFromFile(
        uri: Uri,
        onClientsReady: suspend (List<Client>) -> Unit
    ): ImportResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        var success = 0
        var skipped = 0
        var newBuildingsCount = 0

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ImportResult(0, 0, 0, listOf("Could not open file"))

            val content = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            if (!(content.trimStart().startsWith("<?xml") || content.contains("<Workbook"))) {
                return@withContext ImportResult(0, 0, 0,
                    listOf("Unsupported file format. Please use Excel (.xls) file exported from this app."))
            }

            val buildingDao = db.buildingDao()
            val clientDao = db.clientDao()

            // جلب المباني الموجودة
            val existingBuildings = buildingDao.getAllBuildingsDirect().toMutableList()

            // Parse Excel
            val parsedRows = parseExcelXmlRows(content, errors)
            if (parsedRows.isEmpty()) {
                return@withContext ImportResult(0, 0, 0,
                    errors.ifEmpty { mutableListOf("No valid clients found in file") })
            }

            // التحقق من العملاء المكررين
            val existingClients = clientDao.getAllClientsDirect()
            val existingSubs = existingClients.map { it.subscriptionNumber.lowercase() }.toSet()

            val newClients = mutableListOf<Client>()

            for ((rowIdx, row) in parsedRows.withIndex()) {
                try {
                    val name = row.getOrElse(0) { "" }
                    val subNumber = row.getOrElse(1) { "" }
                    val phone = row.getOrElse(2) { "" }
                    val packageType = row.getOrElse(3) { "5Mbps" }
                    val priceStr = row.getOrElse(4) { "0" }
                    val buildingName = row.getOrElse(5) { "" }
                    val roomNumber = row.getOrElse(6) { "" }.let { if (it == "-" || it.isBlank()) null else it }
                    val startMonth = row.getOrElse(7) { "" }
                    val startDay = row.getOrElse(8) { "1" }
                    val address = row.getOrElse(9) { "" }
                    val notes = row.getOrElse(10) { "" }

                    if (name.isBlank() || subNumber.isBlank()) {
                        errors.add("Row ${rowIdx + 1}: Missing name or subscription number")
                        continue
                    }

                    // تخطي المكررين
                    if (subNumber.lowercase() in existingSubs) {
                        skipped++
                        continue
                    }

                    val price = priceStr.replace(",", "").toDoubleOrNull() ?: 0.0
                    val day = startDay.replace(",", "").toIntOrNull() ?: 1

                    // حساب مبلغ الشهر الأول تلقائياً (نفس منطق ClientEditDialog)
                    val firstMonthAmount: Double? = if (day > 1 && price > 0) {
                        val daysInMonth = 30
                        val remainingDays = daysInMonth - day + 1
                        (price / daysInMonth) * remainingDays
                    } else {
                        null
                    }

                    // البحث عن المبنى أو إنشاؤه
                    var building = existingBuildings.firstOrNull {
                        it.name.equals(buildingName, true)
                    }

                    if (building == null && buildingName.isNotBlank()) {
                        // إنشاء المبنى الجديد تلقائياً
                        val newBuilding = Building(name = buildingName)
                        val newId = buildingRepository.insert(newBuilding)
                        building = newBuilding.copy(id = newId.toInt())
                        existingBuildings.add(building)
                        newBuildingsCount++
                    }

                    val buildingId = building?.id ?: 0

                    newClients.add(
                        Client(
                            name = name,
                            subscriptionNumber = subNumber,
                            phone = phone,
                            packageType = packageType,
                            price = price,
                            firstMonthAmount = firstMonthAmount,
                            buildingId = buildingId,
                            roomNumber = roomNumber,
                            startMonth = startMonth.ifBlank {
                                java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                                    .format(java.util.Date())
                            },
                            startDay = day,
                            address = address,
                            notes = notes
                        )
                    )
                    success++
                } catch (e: Exception) {
                    errors.add("Row ${rowIdx + 1}: ${e.message}")
                }
            }

            if (newClients.isNotEmpty()) {
                onClientsReady(newClients)
            }

            return@withContext ImportResult(success, skipped, newBuildingsCount, errors)

        } catch (e: Exception) {
            return@withContext ImportResult(0, 0, 0, listOf("Error: ${e.message}"))
        }
    }

    private fun parseExcelXmlRows(
        content: String,
        errors: MutableList<String>
    ): List<List<String>> {
        val result = mutableListOf<List<String>>()

        val worksheetPattern = Regex("""<Worksheet[^>]*>.*?</Worksheet>""", RegexOption.DOT_MATCHES_ALL)
        val firstWorksheet = worksheetPattern.find(content)?.value ?: return result

        val rowPattern = Regex("""<Row[^>]*>(.*?)</Row>""", RegexOption.DOT_MATCHES_ALL)
        val cellPattern = Regex("""<Data[^>]*>(.*?)</Data>""", RegexOption.DOT_MATCHES_ALL)

        val rows = rowPattern.findAll(firstWorksheet).toList()

        // البحث عن صف Headers
        var headerRowIndex = -1
        for (i in rows.indices) {
            val cells = cellPattern.findAll(rows[i].groupValues[1]).map { it.groupValues[1].trim() }.toList()
            if (cells.any { it.equals("Name", true) || it.equals("Subscription #", true) }) {
                headerRowIndex = i
                break
            }
        }

        if (headerRowIndex == -1) {
            errors.add("Could not find header row (Name, Subscription #, ...)")
            return result
        }

        val dataRows = rows.drop(headerRowIndex + 1)

        for (row in dataRows) {
            val cells = cellPattern.findAll(row.groupValues[1]).map { it.groupValues[1].trim() }.toList()
            if (cells.isEmpty() || cells.size < 5) continue
            if (cells[0].equals("Total", true) || cells[0].startsWith("Total Amount", true)) continue
            result.add(cells)
        }

        return result
    }
}
