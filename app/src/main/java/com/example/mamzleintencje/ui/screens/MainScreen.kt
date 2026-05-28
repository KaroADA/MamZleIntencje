package com.example.mamzleintencje.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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

@OptIn(ExperimentalMaterial3Api::class)
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

    val nestedScrollConnection = remember(currentRoute) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (currentRoute == Screen.Logs.route) {
                    if (available.y < -10) {
                        isBottomBarVisible = false
                    } else if (available.y > 10) {
                        isBottomBarVisible = true
                    }
                } else {
                    isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (currentRoute == Screen.Logs.route) {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .layout { measurable, constraints ->
                                    val horizontalPadding = 16.dp.roundToPx()
                                    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + horizontalPadding))
                                    layout(placeable.width - horizontalPadding, placeable.height) {
                                        placeable.placeRelative(-horizontalPadding, 0)
                                    }
                                }
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val checkIcon: @Composable () -> Unit = {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }

                            var isSearching by remember { mutableStateOf(false) }
                            val focusRequester = remember { FocusRequester() }
                            var fieldHadFocus by remember { mutableStateOf(false) }

                            LaunchedEffect(isSearching) {
                                if (isSearching) {
                                    focusRequester.requestFocus()
                                } else {
                                    fieldHadFocus = false
                                }
                            }

                            FilterChip(
                                selected = isSearching || filterState.searchQuery.isNotEmpty(),
                                onClick = { isSearching = true },
                                label = {
                                    if (isSearching) {
                                        BasicTextField(
                                            value = filterState.searchQuery,
                                            onValueChange = { query -> viewModel.updateFilter { it.copy(searchQuery = query) } },
                                            modifier = Modifier
                                                .widthIn(min = 40.dp, max = 150.dp)
                                                .focusRequester(focusRequester)
                                                .onFocusChanged {
                                                    if (it.isFocused) {
                                                        fieldHadFocus = true
                                                    } else if (fieldHadFocus) {
                                                        isSearching = false
                                                    }
                                                },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                                        )
                                    } else {
                                        if (filterState.searchQuery.isEmpty()) {
                                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(FilterChipDefaults.IconSize))
                                        } else {
                                            Text(filterState.searchQuery)
                                        }
                                    }
                                },
                                trailingIcon = if (isSearching || filterState.searchQuery.isNotEmpty()) {
                                    {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(FilterChipDefaults.IconSize).clickable {
                                                viewModel.updateFilter { it.copy(searchQuery = "") }
                                                isSearching = false
                                            }
                                        )
                                    }
                                } else null
                            )

                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                            FilterChip(
                                selected = filterState.hideSystemApps,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.updateFilter { it.copy(hideSystemApps = !it.hideSystemApps) }
                                },
                                label = { Text("Hide System") },
                                leadingIcon = if (filterState.hideSystemApps) checkIcon else null
                            )

                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                            FilterChip(
                                selected = filterState.requiresPermission != null,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.updateFilter {
                                        it.copy(requiresPermission = when (it.requiresPermission) {
                                            null -> true
                                            true -> false
                                            false -> null
                                        })
                                    }
                                },
                                label = { Text("Permission") },
                                leadingIcon = when (filterState.requiresPermission) {
                                    true -> { { Icon(Icons.Default.Check, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } }
                                    false -> { { Icon(Icons.Default.Close, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } }
                                    null -> null
                                }
                            )

                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                            FilterChip(
                                selected = filterState.minCvss == 7.0,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.updateFilter { it.copy(minCvss = if (it.minCvss == 7.0) 0.0 else 7.0) }
                                },
                                label = { Text("High Risk") },
                                leadingIcon = if (filterState.minCvss >= 7.0) checkIcon else null
                            )
                            FilterChip(
                                selected = filterState.minCvss >= 4.0 && filterState.minCvss < 7.0,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.updateFilter { it.copy(minCvss = if (it.minCvss >= 4.0 && it.minCvss < 7.0) 0.0 else 4.0) }
                                },
                                label = { Text("Warnings") },
                                leadingIcon = if (filterState.minCvss >= 4.0 && filterState.minCvss < 7.0) checkIcon else null
                            )

                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                            FilterChip(
                                selected = filterState.hasExtras,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.updateFilter { it.copy(hasExtras = !it.hasExtras) }
                                },
                                label = { Text("With Extras") },
                                leadingIcon = if (filterState.hasExtras) checkIcon else null
                            )

                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                            IntentType.values().forEach { type ->
                                FilterChip(
                                    selected = filterState.intentType == type,
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.updateFilter { it.copy(intentType = if (it.intentType == type) null else type) }
                                    },
                                    label = { Text(type.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                                    leadingIcon = if (filterState.intentType == type) checkIcon else null
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

                            val allStatuses = listOf("DELIVERED", "PARTIALLY_SKIPPED", "SKIPPED", "DEFERRED")
                            allStatuses.forEach { status ->
                                FilterChip(
                                    selected = filterState.allowedStatuses.contains(status),
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.updateFilter {
                                            val newSet = if (it.allowedStatuses.contains(status)) {
                                                it.allowedStatuses - status
                                            } else {
                                                it.allowedStatuses + status
                                            }
                                            it.copy(allowedStatuses = newSet)
                                        }
                                    },
                                    label = {
                                        Text(
                                            status.lowercase()
                                                .replace("_", " ")
                                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                        )
                                    },
                                    leadingIcon = if (filterState.allowedStatuses.contains(status)) checkIcon else null
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
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
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    CustomFloatingNavBar(
                        screens = screens,
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
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
