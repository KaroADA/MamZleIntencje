package com.example.mamzleintencje.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mamzleintencje.data.IntentType
import com.example.mamzleintencje.ui.navigation.NavGraph
import com.example.mamzleintencje.ui.navigation.Screen
import com.example.mamzleintencje.ui.viewmodel.MainViewModel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Logs, Screen.Dashboard, Screen.Settings)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val filterState by viewModel.filterState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val focusManager = LocalFocusManager.current

    var isBottomBarVisible by remember { mutableStateOf(true) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    // Check if any filter (other than search) is active to show a badge
    val hasActiveFilters = remember(filterState) {
        filterState.hideSystemApps ||
                filterState.requiresPermission != null ||
                filterState.minCvss > 0.0 ||
                filterState.hasExtras ||
                filterState.intentType != null ||
                filterState.allowedStatuses.size < 4 // Assuming 4 is the total number of statuses
    }

    val nestedScrollConnection = remember(currentRoute) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Show bar when pulling down, regardless of if content scrolls
                if (available.y > 10) isBottomBarVisible = true
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Hide bar ONLY if content actually scrolls up
                if (consumed.y < -10 && (currentRoute == Screen.Logs.route || currentRoute == Screen.Settings.route)) {
                    isBottomBarVisible = false
                }
                return Offset.Zero
            }
        }
    }

    // Reset visibility when navigating between screens
    LaunchedEffect(currentRoute) {
        isBottomBarVisible = true
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (currentRoute == Screen.Logs.route) {
                val focusRequester = remember { FocusRequester() }

                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = filterState.searchQuery,
                            onValueChange = { query -> viewModel.updateFilter { it.copy(searchQuery = query) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .focusRequester(focusRequester),
                            placeholder = { Text("Search intents...", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp)) },
                            trailingIcon = if (filterState.searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = {
                                        viewModel.updateFilter { it.copy(searchQuery = "") }
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                                    }
                                }
                            } else null,
                            singleLine = true,
                            shape = CircleShape,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    },
                    actions = {
                        BadgedBox(
                            badge = {
                                if (hasActiveFilters) {
                                    Badge(modifier = Modifier.offset(x = (-8).dp, y = 8.dp))
                                }
                            }
                        ) {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                showFilterSheet = true
                            }) {
                                Icon(Icons.Default.Tune, contentDescription = "Filters")
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { focusManager.clearFocus() }
                    )
            ) {
                NavGraph(navController = navController, viewModel = viewModel)
            }

            // Bottom Navigation Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .blur(10.dp)
            )

            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                    CustomFloatingNavBar(
                        screens = screens,
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for Filters
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            FilterBottomSheetContent(
                filterState = filterState,
                onFilterChange = { viewModel.updateFilter(it) },
                onClearAll = {
                    viewModel.updateFilter {
                        it.copy(
                            hideSystemApps = false,
                            requiresPermission = null,
                            minCvss = 0.0,
                            hasExtras = false,
                            intentType = null,
                            allowedStatuses = setOf("DELIVERED", "PARTIALLY_SKIPPED", "SKIPPED", "DEFERRED")
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheetContent(
    filterState: MainViewModel.FilterState, // Adjust package if needed
    onFilterChange: ((MainViewModel.FilterState) -> MainViewModel.FilterState) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClearAll) {
                Text("Clear All")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hide System Apps", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = filterState.hideSystemApps,
                onCheckedChange = { onFilterChange { state -> state.copy(hideSystemApps = it) } }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contains Extras", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = filterState.hasExtras,
                onCheckedChange = { onFilterChange { state -> state.copy(hasExtras = it) } }
            )
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // Permissions
        Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "All", true to "Required", false to "None").forEach { (value, label) ->
                FilterChip(
                    selected = filterState.requiresPermission == value,
                    onClick = { onFilterChange { it.copy(requiresPermission = value) } },
                    label = { Text(label) }
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // CVSS Risk
        Text("Vulnerability Risk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filterState.minCvss >= 7.0,
                onClick = { onFilterChange { it.copy(minCvss = if (it.minCvss >= 7.0) 0.0 else 7.0) } },
                label = { Text("High Risk (7.0+)") }
            )
            FilterChip(
                selected = filterState.minCvss in 4.0..6.9,
                onClick = { onFilterChange { it.copy(minCvss = if (it.minCvss in 4.0..6.9) 0.0 else 4.0) } },
                label = { Text("Warnings (4.0 - 6.9)") }
            )
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // Intent Types
        Text("Intent Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            IntentType.entries.forEach { type ->
                FilterChip(
                    selected = filterState.intentType == type,
                    onClick = { onFilterChange { it.copy(intentType = if (it.intentType == type) null else type) } },
                    label = { Text(type.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) }
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // Delivery Status
        Text("Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val allStatuses = listOf("DELIVERED", "PARTIALLY_SKIPPED", "SKIPPED", "DEFERRED")
            allStatuses.forEach { status ->
                val isSelected = filterState.allowedStatuses.contains(status)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onFilterChange {
                            val newSet = if (isSelected) it.allowedStatuses - status else it.allowedStatuses + status
                            it.copy(allowedStatuses = newSet)
                        }
                    },
                    label = {
                        Text(status.lowercase().replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                    }
                )
            }
        }
    }
}

@Composable
fun CustomFloatingNavBar(
    screens: List<Screen>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val density = LocalDensity.current
    var selectedItemIndex by remember { mutableIntStateOf(0) }

    val itemPositions = remember { mutableStateListOf<Float>() }
    if (itemPositions.isEmpty()) {
        repeat(screens.size) { itemPositions.add(0f) }
    }

    LaunchedEffect(currentRoute) {
        val index = screens.indexOfFirst { it.route == currentRoute }
        if (index != -1) selectedItemIndex = index
    }

    val indicatorOffset by animateFloatAsState(
        targetValue = if (itemPositions.size > selectedItemIndex) itemPositions[selectedItemIndex] else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "indicatorOffset"
    )

    Surface(
        modifier = Modifier
            .shadow(12.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp)),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(64.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorOffset.roundToInt(), 0) }
                    .width(64.dp)
                    .height(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEachIndexed { index, screen ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { layoutCoordinates ->
                                val parentX = layoutCoordinates.positionInParent().x
                                val itemWidth = layoutCoordinates.size.width
                                itemPositions[index] = parentX + (itemWidth / 2) - with(density) { 32.dp.toPx() }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigate(screen.route) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = if (currentRoute == screen.route)
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}