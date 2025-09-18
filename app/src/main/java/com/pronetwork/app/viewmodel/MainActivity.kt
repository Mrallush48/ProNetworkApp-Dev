package com.pronetwork.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pronetwork.app.data.BuildingDatabase
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.repository.BuildingRepository
import com.pronetwork.app.repository.ClientRepository
import com.pronetwork.app.ui.screens.BuildingListScreen
import com.pronetwork.app.ui.screens.ClientListScreen
import com.pronetwork.app.viewmodel.BuildingViewModel
import com.pronetwork.app.viewmodel.BuildingViewModelFactory
import com.pronetwork.app.viewmodel.ClientViewModel
import com.pronetwork.app.viewmodel.ClientViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // prepare DBs & repos
        val buildingDao = BuildingDatabase.getDatabase(this).buildingDao()
        val buildingRepo = BuildingRepository(buildingDao)

        val clientDao = ClientDatabase.getDatabase(this).clientDao()
        val clientRepo = ClientRepository(clientDao)

        val buildingViewModelFactory = BuildingViewModelFactory(buildingRepo)
        val clientViewModelFactory = ClientViewModelFactory(clientRepo)

        setContent {
            AppContent(buildingViewModelFactory, clientViewModelFactory)
        }
    }
}

@Composable
fun AppContent(
    buildingViewModelFactory: BuildingViewModelFactory,
    clientViewModelFactory: ClientViewModelFactory
) {
    // viewmodels
    val buildingViewModel: BuildingViewModel = viewModel(factory = buildingViewModelFactory)
    val clientViewModel: ClientViewModel = viewModel(factory = clientViewModelFactory)

    // navigation & state
    var selectedBuildingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var screen by rememberSaveable { mutableStateOf("buildings") }

    // current month in ISO "yyyy-MM"
    val defaultMonthIso = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    var selectedMonthIso by rememberSaveable { mutableStateOf(defaultMonthIso) }

    when (screen) {
        "buildings" -> BuildingListScreen(viewModel = buildingViewModel, onBuildingSelected = { building ->
            selectedBuildingId = building.id
            // set month in clientViewModel when entering clients screen
            clientViewModel.setBuildingAndMonth(building.id, selectedMonthIso)
            screen = "clients"
        })

        "clients" -> {
            val bId = selectedBuildingId ?: return
            ClientListScreen(
                viewModel = clientViewModel,
                buildingId = bId,
                selectedMonthIso = selectedMonthIso,
                onMonthChanged = { iso ->
                    // user changed month in clients screen
                    selectedMonthIso = iso
                    clientViewModel.setBuildingAndMonth(bId, iso)
                },
                onBack = {
                    screen = "buildings"
                }
            )
        }
    }
}
