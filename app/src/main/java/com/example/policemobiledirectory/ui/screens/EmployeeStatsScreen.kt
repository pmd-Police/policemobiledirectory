package com.example.policemobiledirectory.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import java.io.File
import java.io.FileOutputStream

// ----------------- CSV Export Helpers -----------------
fun String?.csvEscape(): String = if (this.isNullOrBlank()) "" else if (contains(",") || contains("\"") || contains("\n")) "\"${replace("\"", "\"\"")}\"" else this

fun createCsvString(employees: List<Employee>): String {
    val sb = StringBuilder()
    sb.append("KGID,Name,Rank,District,Station,Mobile 1,Mobile 2,Email,Blood Group,Metal Number\n")
    employees.forEach { emp ->
        sb.append(
            listOf(
                emp.kgid, emp.name, emp.displayRank, emp.district, emp.station,
                emp.mobile1, emp.mobile2 ?: "", emp.email, emp.bloodGroup, emp.metalNumber ?: ""
            ).joinToString(",") { it.csvEscape() } + "\n"
        )
    }
    return sb.toString()
}

fun shareCsv(employees: List<Employee>, fileName: String, context: Context) {
    if (employees.isEmpty()) {
        Toast.makeText(context, "No data to export for $fileName.", Toast.LENGTH_LONG).show()
        return
    }
    try {
        val csvFile = File(context.cacheDir, "$fileName.csv")
        FileOutputStream(csvFile).use { it.write(createCsvString(employees).toByteArray()) }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share $fileName"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error sharing data: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// ----------------- Employee Stats Screen -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeStatsScreen(
    navController: NavController,
    viewModel: EmployeeViewModel,
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    val allEmployees by viewModel.employees.collectAsState()
    val context = LocalContext.current
    
    // Get dynamic constants from ConstantsViewModel
    val districts by constantsViewModel.districts.collectAsStateWithLifecycle()
    val stationsByDistrict by constantsViewModel.stationsByDistrict.collectAsStateWithLifecycle()
    val ranks by constantsViewModel.ranks.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshEmployees() }

    // --- Filters ---
    var selectedDistrict by remember { mutableStateOf("All") }
    var districtExpanded by remember { mutableStateOf(false) }
    val districtOptions = remember(districts) { listOf("All") + districts }

    var selectedStation by remember { mutableStateOf("All") }
    var stationExpanded by remember { mutableStateOf(false) }
    val stationOptions = remember(selectedDistrict, allEmployees, stationsByDistrict) {
        if (selectedDistrict == "All") {
            listOf("All") + allEmployees.mapNotNull { it.station?.trim() }.distinct().sorted()
        } else {
            val stations = stationsByDistrict[selectedDistrict]
                ?: stationsByDistrict.keys.find { it.equals(selectedDistrict, ignoreCase = true) }?.let { stationsByDistrict[it] }
                ?: emptyList()
            listOf("All") + stations
        }
    }

    var selectedRank by remember { mutableStateOf("All") }
    var rankExpanded by remember { mutableStateOf(false) }
    val allRanksOptions = remember(ranks) { listOf("All") + ranks }

    // --- Filtered employees ---
    val filteredEmployees by remember(allEmployees, selectedDistrict, selectedStation, selectedRank) {
        derivedStateOf {
            allEmployees
                .filter { selectedDistrict == "All" || it.district == selectedDistrict }
                .filter { selectedStation == "All" || it.station == selectedStation }
                .filter { selectedRank == "All" || it.displayRank == selectedRank }
        }
    }

    // --- Statistics ---
    val employeesByDistrictStats = filteredEmployees
        .groupingBy { it.district?.trim()?.ifEmpty { "N/A" } ?: "N/A" }
        .eachCount().toList().sortedByDescending { it.second }

    val employeesByStationStats = filteredEmployees
        .groupingBy { it.station?.trim()?.ifEmpty { "N/A" } ?: "N/A" }
        .eachCount().toList().sortedByDescending { it.second }

    val employeesByRankStats = filteredEmployees
        .groupingBy { it.displayRank.trim().ifEmpty { "N/A" } }
        .eachCount().toList().sortedByDescending { it.second }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employee Statistics") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
        ) {

            // --- Filters UI ---
            FilterDropdown("District/Unit", selectedDistrict, districtOptions, districtExpanded, { districtExpanded = it }) { newDistrict ->
                selectedDistrict = newDistrict
                if (newDistrict != "All") selectedStation = "All"
            }
            Spacer(Modifier.height(8.dp))
            FilterDropdown("Station/Unit", selectedStation, stationOptions, stationExpanded, { stationExpanded = it }) { selectedStation = it }
            Spacer(Modifier.height(8.dp))
            FilterDropdown("Rank", selectedRank, allRanksOptions, rankExpanded, { rankExpanded = it }) { selectedRank = it }
            Spacer(Modifier.height(16.dp))

            when {
                allEmployees.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No employee data available.")
                }
                filteredEmployees.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No employees match current filters.")
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { StatsSection("Employees by District", employeesByDistrictStats, filteredEmployees, context) }
                    item { StatsSection("Employees by Station/Unit", employeesByStationStats, filteredEmployees, context) }
                    item { StatsSection("Employees by Rank", employeesByRankStats, filteredEmployees, context) }
                }
            }
        }
    }
}

// ----------------- Filter Dropdown -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    selected: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

// ----------------- Stats Section -----------------
@Composable
private fun StatsSection(
    title: String,
    data: List<Pair<String, Int>>,
    filteredEmployees: List<Employee>,
    context: Context
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { shareCsv(filteredEmployees, title.replace(" ", "_"), context) }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share $title")
                }
            }
            Spacer(Modifier.height(8.dp))
            if (data.isEmpty()) Text("No data for this category.")
            else data.forEachIndexed { index, (item, count) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item, style = MaterialTheme.typography.bodyMedium)
                    Text(count.toString(), style = MaterialTheme.typography.bodyMedium)
                }
                if (index < data.size - 1) HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
