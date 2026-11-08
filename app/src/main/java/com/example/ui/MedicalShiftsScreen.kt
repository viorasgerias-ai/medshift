package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ShiftEntity

@Composable
fun MedicalShiftsScreen(
    viewModel: ShiftViewModel,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceAiState by viewModel.voiceAiState.collectAsStateWithLifecycle()
    var showAddShiftSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddShiftSheet = true },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.fab_new_shift)
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.fab_new_shift),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_shift")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .testTag("shifts_lazy_column"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Hero Header Card with Firebase + Offline Room Indicator & Voice Entry Action
                item(key = "hero_banner") {
                    ClinicalHeroHeader(
                        onCallCount = uiState.onCallCount,
                        onVoiceQuickAdd = { showAddShiftSheet = true },
                        onSignOut = onSignOut
                    )
                }

                // Optional Cloud Sync Notice
                if (!uiState.cloudSyncError.isNullOrBlank()) {
                    item(key = "cloud_sync_error") {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.cloudSyncError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // 2. KPI Summary Grid (Current, Upcoming, Medical, Admin)
                item(key = "kpi_summary_grid") {
                    ShiftSummaryMetricsGrid(
                        currentCount = uiState.totalCurrentCount,
                        upcomingCount = uiState.totalUpcomingCount,
                        medicalCount = uiState.medicalStaffCount,
                        adminCount = uiState.adminStaffCount
                    )
                }

                // 3. Search & Category/Status Filter Bar
                item(key = "search_and_filters") {
                    SearchAndFilterSection(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = viewModel::setFilter
                    )
                }

                // 4. Current Shifts Section
                if (uiState.currentShifts.isNotEmpty()) {
                    item(key = "header_current_shifts") {
                        SectionHeaderRow(
                            title = stringResource(R.string.section_current_shifts),
                            count = uiState.currentShifts.size,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.AccessTime
                        )
                    }

                    items(
                        items = uiState.currentShifts,
                        key = { shift -> "current_${shift.id}" }
                    ) { shift ->
                        ShiftItemCard(
                            shift = shift,
                            onStartShift = { viewModel.startShift(shift) },
                            onCompleteShift = { viewModel.completeShift(shift) },
                            onDeleteShift = { viewModel.deleteShift(shift) }
                        )
                    }
                }

                // 5. Upcoming Shifts Section
                if (uiState.upcomingShifts.isNotEmpty()) {
                    item(key = "header_upcoming_shifts") {
                        SectionHeaderRow(
                            title = stringResource(R.string.section_upcoming_shifts),
                            count = uiState.upcomingShifts.size,
                            accentColor = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.EventAvailable
                        )
                    }

                    items(
                        items = uiState.upcomingShifts,
                        key = { shift -> "upcoming_${shift.id}" }
                    ) { shift ->
                        ShiftItemCard(
                            shift = shift,
                            onStartShift = { viewModel.startShift(shift) },
                            onCompleteShift = { viewModel.completeShift(shift) },
                            onDeleteShift = { viewModel.deleteShift(shift) }
                        )
                    }
                }

                // 6. Completed Shifts Section
                if (uiState.completedShifts.isNotEmpty()) {
                    item(key = "header_completed_shifts") {
                        SectionHeaderRow(
                            title = stringResource(R.string.section_completed_shifts),
                            count = uiState.completedShifts.size,
                            accentColor = MaterialTheme.colorScheme.outline,
                            icon = Icons.Default.CheckCircle
                        )
                    }

                    items(
                        items = uiState.completedShifts,
                        key = { shift -> "completed_${shift.id}" }
                    ) { shift ->
                        ShiftItemCard(
                            shift = shift,
                            onStartShift = { viewModel.startShift(shift) },
                            onCompleteShift = { viewModel.completeShift(shift) },
                            onDeleteShift = { viewModel.deleteShift(shift) }
                        )
                    }
                }

                // 7. Empty State when filters return no shifts
                if (!uiState.isLoading &&
                    uiState.currentShifts.isEmpty() &&
                    uiState.upcomingShifts.isEmpty() &&
                    uiState.completedShifts.isEmpty()
                ) {
                    item(key = "empty_shifts_state") {
                        EmptyShiftsCard(
                            onResetFilters = {
                                viewModel.setFilter(ShiftFilter.ALL)
                                viewModel.setSearchQuery("")
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddShiftSheet) {
        AddShiftBottomSheet(
            voiceAiState = voiceAiState,
            onExtractWithFirebaseAi = viewModel::extractVoiceCommandWithFirebaseAi,
            onResetVoiceAiState = viewModel::resetVoiceAiState,
            onDismiss = { showAddShiftSheet = false },
            onSaveShift = viewModel::addShift
        )
    }
}

@Composable
private fun ClinicalHeroHeader(
    onCallCount: Int,
    onVoiceQuickAdd: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_summary_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_clinic_hero_1794133231657),
                contentDescription = "Central de Controle Clínico",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC061C2B),
                                Color(0xF208263A)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF00A896).copy(alpha = 0.25f),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = Color(0xFF4ADBC8).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(50)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF4ADBC8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Firebase Cloud + Room Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (onCallCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFE06A4E).copy(alpha = 0.9f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "$onCallCount Sobreaviso",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sair da conta Google",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.header_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD2E8F5)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speech-to-Text + Gemini AI",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF9EB8C9)
                    )

                    FilledTonalButton(
                        onClick = onVoiceQuickAdd,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF00A896),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("hero_voice_entry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.voice_quick_entry),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftSummaryMetricsGrid(
    currentCount: Int,
    upcomingCount: Int,
    medicalCount: Int,
    adminCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricSummaryCard(
                title = stringResource(R.string.kpi_current_shifts),
                value = currentCount.toString(),
                subtitle = "Ativos no turno atual",
                icon = Icons.Default.AccessTime,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_card_current")
            )
            MetricSummaryCard(
                title = stringResource(R.string.kpi_upcoming_shifts),
                value = upcomingCount.toString(),
                subtitle = "Próximas 24 horas",
                icon = Icons.Default.Schedule,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_card_upcoming")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricSummaryCard(
                title = stringResource(R.string.kpi_medical_staff),
                value = medicalCount.toString(),
                subtitle = "UTI, Trauma & Clínica",
                icon = Icons.Default.LocalHospital,
                accentColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_card_medical")
            )
            MetricSummaryCard(
                title = stringResource(R.string.kpi_admin_staff),
                value = adminCount.toString(),
                subtitle = "Regulação & Admissão",
                icon = Icons.Default.AdminPanelSettings,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_card_admin")
            )
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: ShiftFilter,
    onFilterSelected: (ShiftFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpar busca"
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_shifts_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShiftFilter.entries.forEach { filter ->
                val selected = filter == selectedFilter
                FilterChip(
                    selected = selected,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    count: Int,
    accentColor: Color,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = accentColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = "$count plantões",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ShiftItemCard(
    shift: ShiftEntity,
    onStartShift: () -> Unit,
    onCompleteShift: () -> Unit,
    onDeleteShift: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrent = shift.shiftStatus == "CURRENT"
    val isUpcoming = shift.shiftStatus == "UPCOMING"
    val isAdmin = shift.category == "ADMIN"

    val statusColor = when (shift.shiftStatus) {
        "CURRENT" -> MaterialTheme.colorScheme.secondary
        "UPCOMING" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shift_card_${shift.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isCurrent) 1.5.dp else 1.dp,
                    color = if (isCurrent) statusColor.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = shift.department,
                                style = MaterialTheme.typography.labelLarge,
                                color = statusColor
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isAdmin) "ADMIN" else "CLÍNICO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (shift.createdViaVoice) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.voice_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    if (shift.isOnCall) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.on_call_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shift.staffName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = shift.roleTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = shift.crmOrRegistry,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = shift.shiftTimeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = shift.dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Room,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = shift.locationRoom,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = shift.permissionLevel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isCurrent, enter = fadeIn()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Andamento do Turno Atual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(shift.progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { shift.progressFraction.coerceIn(0f, 1f) },
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }

            if (shift.notes.isNotBlank()) {
                Text(
                    text = shift.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDeleteShift,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("delete_shift_${shift.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.btn_delete_shift),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_delete_shift))
                }

                if (isUpcoming) {
                    FilledTonalButton(
                        onClick = onStartShift,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("start_shift_${shift.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_start_shift))
                    }
                } else if (isCurrent) {
                    FilledTonalButton(
                        onClick = onCompleteShift,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("complete_shift_${shift.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_complete_shift))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyShiftsCard(onResetFilters: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("empty_shifts_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MedicalServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = stringResource(R.string.empty_shifts_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.empty_shifts_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onResetFilters,
                modifier = Modifier.testTag("reset_filters_button")
            ) {
                Text("Limpar Filtros")
            }
        }
    }
}
