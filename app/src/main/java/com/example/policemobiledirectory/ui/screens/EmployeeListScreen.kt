@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.example.policemobiledirectory.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextAlign
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.ui.theme.*
import com.example.policemobiledirectory.ui.theme.ErrorRed
import com.example.policemobiledirectory.ui.theme.CardShadow
import com.example.policemobiledirectory.ui.theme.BorderTeal
import com.example.policemobiledirectory.ui.theme.ChipSelectedStart
import com.example.policemobiledirectory.ui.theme.ChipSelectedEnd
import com.example.policemobiledirectory.ui.theme.ChipUnselected
import com.example.policemobiledirectory.ui.theme.BorderChipUnselected
import com.example.policemobiledirectory.ui.theme.GlassOpacity
import com.example.policemobiledirectory.ui.theme.PrimaryTeal
import com.example.policemobiledirectory.ui.theme.PrimaryTealDark
import com.example.policemobiledirectory.ui.theme.FABColor
import com.example.policemobiledirectory.ui.theme.BorderTeal
import com.example.policemobiledirectory.ui.theme.BackgroundLight
import com.example.policemobiledirectory.ui.theme.SecondaryYellow
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.launch
import com.example.policemobiledirectory.ui.components.EmployeeCardAdmin
import com.example.policemobiledirectory.ui.components.EmployeeCardUser
import com.example.policemobiledirectory.ui.components.ContactCard
import kotlinx.coroutines.CoroutineScope
import com.example.policemobiledirectory.navigation.Routes
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    navController: NavController,
    viewModel: EmployeeViewModel,
    onThemeToggle: () -> Unit,
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val filteredEmployees by viewModel.filteredEmployees.collectAsStateWithLifecycle()
    val employeeStatus by viewModel.employeeStatus.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Notification counts
    val userNotifications by viewModel.userNotifications.collectAsStateWithLifecycle()
    val adminNotifications by viewModel.adminNotifications.collectAsStateWithLifecycle()
    val userNotificationsSeenAt by viewModel.userNotificationsLastSeen.collectAsStateWithLifecycle()
    val adminNotificationsSeenAt by viewModel.adminNotificationsLastSeen.collectAsStateWithLifecycle()
    val notificationCount = if (isAdmin) {
        adminNotifications.count { (it.timestamp ?: 0L) > adminNotificationsSeenAt }
    } else {
        userNotifications.count { (it.timestamp ?: 0L) > userNotificationsSeenAt }
    }

    // Get the current back stack entry to detect when screen comes back into focus
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    LaunchedEffect(currentRoute) { 
        // Only refresh if we're on the employee list screen
        if (currentRoute == Routes.EMPLOYEE_LIST) {
            viewModel.checkIfAdmin()
            // Refresh data when screen comes back into focus
            viewModel.refreshEmployees()
            viewModel.refreshOfficers()
        }
        // Constants.kt is the primary source - no automatic syncing
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PMD Home")
                        Spacer(Modifier.width(6.dp))
                        Box {
                            IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
                            // Notification badge - circular with white border (Yellow theme)
                            if (notificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .offset(x = 12.dp, y = (-12).dp)
                                        .background(
                                            color = SecondaryYellow, // Yellow for notification badge
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = Color.White,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Will use gradient background
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
                    actionIconContentColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.background(
                    brush = Brush.linearGradient(
                        listOf(
                            PrimaryTeal.copy(alpha = GlassOpacity),
                            PrimaryTealDark.copy(alpha = GlassOpacity)
                        )
                    )
                ),
                actions = {
                    IconButton(onClick = { 
                        viewModel.refreshEmployees()
                        viewModel.refreshOfficers()
                        constantsViewModel.forceRefresh()  // ← Also refresh constants from Google Sheet
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    
                    // Single Font Size Button with Dropdown Menu
                    FontSizeSelectorButton(
                        currentFontScale = fontScale,
                        onFontScaleSelected = { scale ->
                            viewModel.setFontScale(scale)
                        },
                        onFontScaleToggle = {
                            // Cycle through common presets: 0.8, 1.0, 1.2, 1.4, 1.6, 1.8
                            val presets = listOf(0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f)
                            val current = fontScale
                            val currentIndex = presets.indexOfFirst { 
                                kotlin.math.abs(it - current) < 0.05f 
                            }
                            val nextIndex = if (currentIndex >= 0 && currentIndex < presets.size - 1) {
                                currentIndex + 1
                            } else {
                                0 // Cycle back to first
                            }
                            viewModel.setFontScale(presets[nextIndex])
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    IconButton(onClick = onThemeToggle) {
                        Icon(Icons.Default.Brightness6, contentDescription = "Toggle Theme")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                Box(
                    modifier = Modifier.shadow(
                        elevation = 12.dp,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        spotColor = FABColor.copy(alpha = 0.5f),
                        ambientColor = FABColor.copy(alpha = 0.3f)
                    )
                ) {
                    FloatingActionButton(
                        onClick = { navController.navigate("${Routes.ADD_EMPLOYEE}?employeeId=") },
                        containerColor = FABColor,
                        modifier = Modifier.size(64.dp) // Larger size
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Employee",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = BackgroundLight // Light off-white background
        ) {
            EmployeeListContent(
                navController = navController,
                viewModel = viewModel,
                constantsViewModel = constantsViewModel,
                context = context,
                isAdmin = isAdmin,
                fontScale = fontScale,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope
            )
        }
    }
}

@Composable
private fun EmployeeListContent(
    navController: NavController,
    viewModel: EmployeeViewModel,
    constantsViewModel: ConstantsViewModel,
    context: Context,
    isAdmin: Boolean,
    fontScale: Float,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    val filteredEmployees by viewModel.filteredEmployees.collectAsStateWithLifecycle()
    val filteredContacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()
    val employeeStatus by viewModel.employeeStatus.collectAsStateWithLifecycle()
    val officerStatus by viewModel.officerStatus.collectAsStateWithLifecycle()
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // Get constants from ViewModel
    val districts by constantsViewModel.districts.collectAsStateWithLifecycle()
    val stationsByDistrict by constantsViewModel.stationsByDistrict.collectAsStateWithLifecycle()
    val ranks by constantsViewModel.ranks.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf("All") }
    var districtExpanded by remember { mutableStateOf(false) }
    // Show "All" only for admins, regular users see only districts
    val districtsList = remember(isAdmin, districts) {
        if (isAdmin) {
            listOf("All") + districts
        } else {
            districts
        }
    }

    var selectedStation by remember { mutableStateOf("All") }
    var stationExpanded by remember { mutableStateOf(false) }
    val stationsForDistrict = remember(selectedDistrict, stationsByDistrict) {
        if (selectedDistrict == "All") listOf("All")
        else {
            val stations = stationsByDistrict[selectedDistrict]
                ?: stationsByDistrict.keys.find { it.equals(selectedDistrict, ignoreCase = true) }?.let { stationsByDistrict[it] }
                ?: emptyList()
            listOf("All") + stations
        }
    }

    var selectedRank by remember { mutableStateOf("All") }
    var rankExpanded by remember { mutableStateOf(false) }
    val allRanks = remember(ranks) { listOf("All") + ranks }
    val selectedRankState by viewModel.selectedRank.collectAsStateWithLifecycle()
    
    // Sync selectedRank with ViewModel
    LaunchedEffect(selectedRankState) {
        selectedRank = selectedRankState
    }

    // Initialize district to user's registered district when currentUser loads (for non-admins)
    // For admins, default to "All"; for regular users, default to their registered district
    LaunchedEffect(currentUser, isAdmin, districts) {
        if (isAdmin) {
            // Admin: default to "All" if not already set
            if (selectedDistrict == "All") {
                viewModel.updateSelectedDistrict("All")
            }
        } else {
            // Regular user: set to their registered district (if it exists in the list)
            val userDistrict = currentUser?.district?.takeIf { it.isNotBlank() }
            if (userDistrict != null && districts.contains(userDistrict)) {
                // User's district is valid and exists in the list
                selectedDistrict = userDistrict
                viewModel.updateSelectedDistrict(userDistrict)
            } else {
                // Fallback: use first district if user has no district set or district is invalid
                val fallbackDistrict = districts.firstOrNull() ?: "All"
                selectedDistrict = fallbackDistrict
                viewModel.updateSelectedDistrict(fallbackDistrict)
            }
        }
    }

    val searchFields = SearchFilter.values().toList()
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        // 🔸 DISTRICT & STATION DROPDOWNS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // District Dropdown - Glassmorphism style
            ExposedDropdownMenuBox(
                expanded = districtExpanded,
                onExpandedChange = { districtExpanded = !districtExpanded },
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(15.dp),
                        spotColor = CardShadow,
                        ambientColor = CardShadow.copy(alpha = 0.5f)
                    )
            ) {
                OutlinedTextField(
                    value = selectedDistrict,
                    onValueChange = {},
                    readOnly = true,
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryTeal
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("District/Unit")
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryTeal
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BorderTeal,
                        unfocusedBorderColor = BorderTeal,
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = PrimaryTeal
                    )
                )
                ExposedDropdownMenu(
                    expanded = districtExpanded,
                    onDismissRequest = { districtExpanded = false }
                ) {
                    districtsList.forEach { district ->
                        DropdownMenuItem(
                            text = { Text(district) },
                            onClick = {
                                selectedDistrict = district
                                selectedStation = "All"
                                districtExpanded = false
                                viewModel.updateSelectedDistrict(district)
                                viewModel.updateSelectedStation("All")
                            }
                        )
                    }
                }
            }

            // Station Dropdown - Glassmorphism style
            ExposedDropdownMenuBox(
                expanded = stationExpanded,
                onExpandedChange = {
                    if (selectedDistrict != "All") stationExpanded = !stationExpanded
                },
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(15.dp),
                        spotColor = CardShadow,
                        ambientColor = CardShadow.copy(alpha = 0.5f)
                    )
            ) {
                OutlinedTextField(
                    value = selectedStation,
                    onValueChange = {},
                    readOnly = true,
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryTeal
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Station/Unit")
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            tint = PrimaryTeal
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    enabled = selectedDistrict != "All",
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BorderTeal,
                        unfocusedBorderColor = BorderTeal,
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = PrimaryTeal
                    )
                )
                ExposedDropdownMenu(
                    expanded = stationExpanded,
                    onDismissRequest = { stationExpanded = false }
                ) {
                    stationsForDistrict.forEach { station ->
                        DropdownMenuItem(
                            text = { Text(station) },
                            onClick = {
                                selectedStation = station
                                stationExpanded = false
                                viewModel.updateSelectedStation(station)
                            }
                        )
                    }
                }
            }
        }

        // 🔹 RANK DROPDOWN & SEARCH BAR (Same Row - 1/4 and 3/4 width)
        val searchLabel = when (searchFilter) {
            SearchFilter.METAL_NUMBER -> "Metal"
            SearchFilter.KGID -> "KGID"
            SearchFilter.MOBILE -> "Mobile"
            SearchFilter.STATION -> "Station"
            SearchFilter.RANK -> "Rank"
            SearchFilter.NAME -> "Name"
            else -> searchFilter.name.lowercase().replaceFirstChar { it.uppercase() }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Rank Dropdown - Glassmorphism style
            ExposedDropdownMenuBox(
                expanded = rankExpanded,
                onExpandedChange = { rankExpanded = !rankExpanded },
                modifier = Modifier
                    .weight(1.3f)
                    .widthIn(min = 150.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(15.dp),
                        spotColor = CardShadow,
                        ambientColor = CardShadow.copy(alpha = 0.5f)
                    )
            ) {
                OutlinedTextField(
                    value = selectedRank,
                    onValueChange = {},
                    readOnly = true,
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MilitaryTech,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryTeal
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Rank")
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = PrimaryTeal
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor().height(56.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BorderTeal,
                        unfocusedBorderColor = BorderTeal,
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = PrimaryTeal
                    )
                )
                ExposedDropdownMenu(
                    expanded = rankExpanded,
                    onDismissRequest = { rankExpanded = false }
                ) {
                    allRanks.forEach { rank ->
                        DropdownMenuItem(
                            text = { Text(rank) },
                            onClick = {
                                selectedRank = rank
                                rankExpanded = false
                                viewModel.updateSelectedRank(rank)
                            }
                        )
                    }
                }
            }

            // Search Box - 3/4 width
            // Determine keyboard type based on selected filter
            val keyboardType = when (searchFilter) {
                SearchFilter.MOBILE, SearchFilter.METAL_NUMBER -> KeyboardType.Number
                else -> KeyboardType.Text
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.updateSearchQuery(it)
                },
                placeholder = { Text("Search by $searchLabel") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = "Search",
                        tint = PrimaryTeal
                    ) 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.updateSearchQuery("")
                        }) { 
                            Icon(
                                Icons.Default.Clear, 
                                contentDescription = "Clear",
                                tint = PrimaryTeal
                            ) 
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType
                ),
                modifier = Modifier
                    .weight(2.7f)
                    .height(56.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(15.dp),
                        spotColor = CardShadow,
                        ambientColor = CardShadow.copy(alpha = 0.5f)
                    ),
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorderTeal,
                    unfocusedBorderColor = BorderTeal,
                    focusedLabelColor = PrimaryTeal,
                    unfocusedLabelColor = PrimaryTeal
                )
            )
        }

        // 🔹 FILTER CHIPS (Compact Wrapping Layout)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp)
        ) {
            searchFields.forEach { filter ->
                // Skip KGID for non-admins
                if (filter == SearchFilter.KGID && !isAdmin) return@forEach
                // Skip RANK since we have a rank dropdown
                if (filter == SearchFilter.RANK) return@forEach

                val selected = searchFilter == filter
                val labelText = when (filter) {
                    SearchFilter.METAL_NUMBER -> "Metal"
                    SearchFilter.KGID -> "KGID"
                    else -> filter.name.lowercase().replaceFirstChar { it.uppercase() }
                }

                // Glassmorphism chip with gradient for selected
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = if (selected) 4.dp else 0.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = if (selected) CardShadow else Color.Transparent,
                            ambientColor = if (selected) CardShadow.copy(alpha = 0.5f) else Color.Transparent
                        )
                ) {
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.updateSearchFilter(filter) },
                        enabled = true,
                        label = {
                            Text(
                                labelText,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (selected) Color.Transparent else ChipUnselected,
                            selectedLabelColor = Color.White,
                            containerColor = ChipUnselected,
                            labelColor = if (selected) Color.White else PrimaryTeal
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = BorderChipUnselected,
                            disabledBorderColor = BorderChipUnselected,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = if (selected) 0.dp else 1.dp,
                            selectedBorderWidth = 0.dp
                        ),
                        modifier = if (selected) {
                            Modifier.background(
                                brush = Brush.linearGradient(
                                    listOf(ChipSelectedStart, ChipSelectedEnd)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }


        // 🔹 UNIFIED CONTACTS LIST (Employees + Officers)
        Box(modifier = Modifier.weight(1f)) {
            val isLoading = employeeStatus is OperationStatus.Loading || officerStatus is OperationStatus.Loading
            val hasError = employeeStatus is OperationStatus.Error || officerStatus is OperationStatus.Error
            val errorMessage = when {
                employeeStatus is OperationStatus.Error -> (employeeStatus as OperationStatus.Error).message
                officerStatus is OperationStatus.Error -> (officerStatus as OperationStatus.Error).message
                else -> null
            }
            
            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                hasError -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Error: ${errorMessage ?: "Unknown error"}", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    if (filteredContacts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                if (allContacts.isEmpty()) {
                                    Text(
                                        "No contacts available.",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "Try refreshing the list.",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                } else {
                                    Text(
                                        "No contacts match your filters.",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "Try adjusting district, station, rank, or search filters.",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.updateSelectedDistrict("All")
                                            viewModel.updateSelectedStation("All")
                                            viewModel.updateSelectedRank("All")
                                            viewModel.updateSearchQuery("")
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryTeal
                                        )
                                    ) {
                                        Text("Reset All Filters")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredContacts, key = { it.id }) { contact ->
                                if (isAdmin && contact.employee != null) {
                                    // 🧑‍💼 Show admin version for employees only
                                    EmployeeCardAdmin(
                                        employee = contact.employee,
                                        isAdmin = true,
                                        fontScale = fontScale,
                                        navController = navController,
                                        onDelete = { employeeToDelete = contact.employee },
                                        context = context
                                    )
                                } else {
                                    // 👮‍♂️ Show unified contact card (works for both Employee and Officer)
                                    ContactCard(
                                        employee = contact.employee,
                                        officer = contact.officer,
                                        fontScale = fontScale,
                                        onClick = {
                                            Toast.makeText(context, "${contact.name} selected", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🔹 DELETE CONFIRMATION + SNACKBAR (Improved with Auto Refresh)
        if (employeeToDelete != null) {
            AlertDialog(
                onDismissRequest = { employeeToDelete = null },
                title = { Text("Confirm Delete") },
                text = { Text("Delete ${employeeToDelete?.name}? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        val emp = employeeToDelete
                        employeeToDelete = null
                        if (emp != null) {
                            coroutineScope.launch {
                                try {
                                    // ✅ Delete and refresh the employee list
                                    viewModel.deleteEmployee(emp.kgid, emp.photoUrl)
                                    viewModel.refreshEmployees()

                                    // ✅ Show success message
                                    snackbarHostState.showSnackbar(
                                        message = "${emp.name} deleted successfully",
                                        withDismissAction = true
                                    )
                                } catch (e: Exception) {
                                    // 🚨 Handle failure gracefully
                                    snackbarHostState.showSnackbar(
                                        message = "Failed to delete ${emp.name}: ${e.message}",
                                        withDismissAction = true
                                    )
                                }
                            }
                        }
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { employeeToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

    }
}

/**
 * Single Font Size Selector Button
 * - Click to cycle through common preset sizes (0.8, 1.0, 1.2, 1.4, 1.6, 1.8)
 * - Long press opens menu with all size options for precise selection
 */
@Composable
private fun FontSizeSelectorButton(
    currentFontScale: Float,
    onFontScaleSelected: (Float) -> Unit,
    onFontScaleToggle: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    val presetSizes = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f)
    
    Box {
        // Single button with combined clickable for click and long press
        Box(
            modifier = Modifier
                .combinedClickable(
                    onClick = onFontScaleToggle,
                    onLongClick = { showDropdownMenu = true }
                )
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${(currentFontScale * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Font Size (Click to cycle, Long press for menu)",
                    modifier = Modifier.size(18.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
        }
        
        // Dropdown Menu
        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false }
        ) {
            presetSizes.forEach { size ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${(size * 100).toInt()}%",
                            fontWeight = if (kotlin.math.abs(size - currentFontScale) < 0.05f) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    onClick = {
                        onFontScaleSelected(size)
                        showDropdownMenu = false
                    },
                    leadingIcon = if (kotlin.math.abs(size - currentFontScale) < 0.05f) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Current size",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}
