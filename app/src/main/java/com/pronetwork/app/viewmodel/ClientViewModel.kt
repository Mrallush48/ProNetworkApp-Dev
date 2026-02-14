package com.pronetwork.app.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.repository.ClientRepository
import kotlinx.coroutines.launch

class ClientViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ClientRepository
    private val _searchQuery = MutableLiveData("")
    val clients: LiveData<List<Client>>
    val clientsCount: LiveData<Int>
    val paidClientsCount: LiveData<Int>
    val unpaidClientsCount: LiveData<Int>

    init {
        val clientDao = ClientDatabase.getDatabase(application).clientDao()
        repository = ClientRepository(clientDao)
        clients = _searchQuery.switchMap { query ->
            if (query.isEmpty()) repository.clients else repository.searchClients(query)
        }
        clientsCount = repository.getClientsCount()
        paidClientsCount = repository.clients.map { list -> list.count { it.isPaid } }
        unpaidClientsCount = repository.clients.map { list -> list.count { !it.isPaid } }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun insert(client: Client) = viewModelScope.launch { repository.insert(client) }
    fun update(client: Client) = viewModelScope.launch { repository.update(client) }
    fun delete(client: Client) = viewModelScope.launch { repository.delete(client) }

    fun exportClientsToCSV(
        clients: List<Client>,
        buildings: List<Building>
    ): String {
        val csvBuilder = StringBuilder()

        // العناوين (Headers)
        csvBuilder.appendLine("Name,Subscription Number,Phone,Package,Price,Building,Start Month,Start Day,Address,Notes")

        // البيانات
        clients.forEach { client ->
            val buildingName = buildings.find { it.id == client.buildingId }?.name ?: "Unknown"
            csvBuilder.appendLine(
                "${client.name},${client.subscriptionNumber},${client.phone},${client.packageType}," +
                        "${client.price},${buildingName},${client.startMonth},${client.startDay}," +
                        "\"${client.address}\",\"${client.notes}\""
            )
        }

        return csvBuilder.toString()
    }
    fun exportClientsToExcelHTML(
        clients: List<Client>,
        buildings: List<Building>
    ): String {
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val total = clients.sumOf { it.price }

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")

            // Styles
            appendLine("""<Styles>""")
            appendLine("""<Style ss:ID="Title"><Font ss:Bold="1" ss:Size="18" ss:Color="#673AB7"/><Alignment ss:Horizontal="Center"/></Style>""")
            appendLine("""<Style ss:ID="Subtitle"><Font ss:Italic="1" ss:Size="10" ss:Color="#666666"/><Alignment ss:Horizontal="Center"/></Style>""")
            appendLine("""<Style ss:ID="Header"><Font ss:Bold="1" ss:Size="11" ss:Color="#FFFFFF"/><Interior ss:Color="#673AB7" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center" ss:Vertical="Center"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#000000"/><Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1"/></Borders></Style>""")
            appendLine("""<Style ss:ID="EvenRow"><Interior ss:Color="#EDE7F6" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/></Borders></Style>""")
            appendLine("""<Style ss:ID="OddRow"><Interior ss:Color="#FFFFFF" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/></Borders></Style>""")
            appendLine("""<Style ss:ID="CurrencyEven"><NumberFormat ss:Format="#,##0.00"/><Font ss:Color="#673AB7"/><Interior ss:Color="#EDE7F6" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/></Borders></Style>""")
            appendLine("""<Style ss:ID="CurrencyOdd"><NumberFormat ss:Format="#,##0.00"/><Font ss:Color="#673AB7"/><Interior ss:Color="#FFFFFF" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/><Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/></Borders></Style>""")
            appendLine("""<Style ss:ID="Total"><Font ss:Bold="1" ss:Size="12" ss:Color="#FFFFFF"/><Interior ss:Color="#9575CD" ss:Pattern="Solid"/><NumberFormat ss:Format="#,##0.00"/><Alignment ss:Horizontal="Right"/></Style>""")
            appendLine("""<Style ss:ID="TotalLabel"><Font ss:Bold="1" ss:Size="12" ss:Color="#FFFFFF"/><Interior ss:Color="#9575CD" ss:Pattern="Solid"/><Alignment ss:Horizontal="Right"/></Style>""")
            appendLine("""</Styles>""")

            appendLine("""<Worksheet ss:Name="Clients">""")
            appendLine("""<Table ss:DefaultColumnWidth="100">""")

            // Column widths
            appendLine("""<Column ss:Width="150"/>""") // Name
            appendLine("""<Column ss:Width="120"/>""") // Subscription
            appendLine("""<Column ss:Width="100"/>""") // Phone
            appendLine("""<Column ss:Width="80"/>""")  // Package
            appendLine("""<Column ss:Width="80"/>""")  // Price
            appendLine("""<Column ss:Width="120"/>""") // Building
            appendLine("""<Column ss:Width="90"/>""")  // Start Month
            appendLine("""<Column ss:Width="70"/>""")  // Start Day
            appendLine("""<Column ss:Width="150"/>""") // Address
            appendLine("""<Column ss:Width="200"/>""") // Notes

            // Title row
            appendLine("""<Row ss:Height="30">""")
            appendLine("""<Cell ss:StyleID="Title" ss:MergeAcross="9"><Data ss:Type="String">📊 Pro Network Spot - Clients Report</Data></Cell>""")
            appendLine("""</Row>""")

            // Subtitle
            appendLine("""<Row ss:Height="18">""")
            appendLine("""<Cell ss:StyleID="Subtitle" ss:MergeAcross="9"><Data ss:Type="String">Generated: $currentDate | Total: ${clients.size} clients</Data></Cell>""")
            appendLine("""</Row>""")

            appendLine("""<Row ss:Height="10"/>""")

            // Header row
            appendLine("""<Row ss:Height="25">""")
            listOf("Name", "Subscription #", "Phone", "Package", "Price (SAR)", "Building", "Start Month", "Day", "Address", "Notes").forEach {
                appendLine("""<Cell ss:StyleID="Header"><Data ss:Type="String">$it</Data></Cell>""")
            }
            appendLine("""</Row>""")

            // Data rows
            clients.forEachIndexed { index, client ->
                val buildingName = buildings.find { it.id == client.buildingId }?.name ?: "N/A"
                val rowStyle = if (index % 2 == 0) "EvenRow" else "OddRow"
                val currencyStyle = if (index % 2 == 0) "CurrencyEven" else "CurrencyOdd"

                appendLine("""<Row ss:Height="20">""")
                appendLine("""<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.name}</Data></Cell>""")
                appendLine("""<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.subscriptionNumber}</Data></Cell>""")
                appendLine("""<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.phone}</Data></Cell>""")
                appendLine("""<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.packageType}</Data></Cell>""")
                appendLine("""<Cell ss:StyleID="$currencyStyle"><Data ss:Type="Number">${client.price}</Data></Cell>""")
                appendLine("""<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">$buildingName</Data></Cell>""")
                appendLine("""<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.startMonth}</Data></Cell>""")
                appendLine("""<Cell ss:StyleID="$rowStyle"><Data ss:Type="Number">${client.startDay}</Data></Cell>""")
                appendLine("""<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.address}</Data></Cell>""")
                appendLine("""<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${client.notes}</Data></Cell>""")
                appendLine("""</Row>""")
            }

            appendLine("""<Row ss:Height="5"/>""")

            // Total row
            appendLine("""<Row ss:Height="25">""")
            appendLine("""<Cell ss:StyleID="TotalLabel" ss:MergeAcross="3"><Data ss:Type="String">Total Amount:</Data></Cell>""")
            appendLine("""<Cell ss:StyleID="Total"><Data ss:Type="Number">$total</Data></Cell>""")
            appendLine("""<Cell ss:StyleID="TotalLabel" ss:MergeAcross="4"><Data ss:Type="String">SAR</Data></Cell>""")
            appendLine("""</Row>""")

            appendLine("""</Table>""")
            appendLine("""</Worksheet>""")
            appendLine("""</Workbook>""")
        }
    }

}