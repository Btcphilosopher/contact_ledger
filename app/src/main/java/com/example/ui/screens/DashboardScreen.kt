package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.ui.theme.IntensityMedium
import com.example.ui.theme.IntensityStrong
import com.example.ui.theme.IntensityWeak
import com.example.ui.viewmodel.ContactLedgerViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ContactLedgerViewModel,
    onContactClick: (Int) -> Unit,
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.allContacts.collectAsState()
    val filteredContacts by viewModel.filteredContacts.collectAsState()
    val tags by viewModel.allTagsList.collectAsState()
    val locations by viewModel.allLocationsList.collectAsState()
    
    // States observing filters
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeTag by viewModel.selectedFilterTag.collectAsState()
    val activeLocation by viewModel.selectedLocationFilter.collectAsState()
    val activeCloseness by viewModel.selectedClosenessFilter.collectAsState()
    val overdueOnly by viewModel.showOnlyOverdue.collectAsState()

    val overdueCount = remember(contacts) { contacts.count { it.isFollowUpOverdue() } }
    val strongTiesCount = remember(contacts) { contacts.count { it.closenessScore >= 4 } }

    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_contact_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- HEADER ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Contact Ledger",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Relationship intelligence, tracked locally.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // --- METRIC ROW ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Network Size",
                    value = contacts.size.toString(),
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Overdue",
                    value = overdueCount.toString(),
                    icon = Icons.Default.Timeline,
                    color = if (overdueCount > 0) IntensityWeak else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.showOnlyOverdue.value = !overdueOnly }
                )
                MetricCard(
                    title = "Strong Ties",
                    value = strongTiesCount.toString(),
                    icon = Icons.Default.Star,
                    color = IntensityStrong,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectedClosenessFilter.value = if (activeCloseness == 4) null else 4 }
                )
            }

            // --- SEARCH AND FILTER BUTTONS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search name, company, notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    singleLine = true
                )

                Button(
                    onClick = { showFilterSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTag != null || activeLocation != null || activeCloseness != null || overdueOnly)
                            MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (activeTag != null || activeLocation != null || activeCloseness != null || overdueOnly)
                            MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("filter_button")
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filters")
                }
            }

            // --- ACTIVE FILTERS LIST ---
            AnimatedVisibility(
                visible = activeTag != null || activeLocation != null || activeCloseness != null || overdueOnly,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active filters:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (overdueOnly) {
                            item {
                                FilterBadge(label = "Overdue") { viewModel.showOnlyOverdue.value = false }
                            }
                        }
                        activeTag?.let { tag ->
                            item {
                                FilterBadge(label = "#$tag") { viewModel.selectedFilterTag.value = null }
                            }
                        }
                        activeLocation?.let { loc ->
                            item {
                                FilterBadge(label = "📍 $loc") { viewModel.selectedLocationFilter.value = null }
                            }
                        }
                        activeCloseness?.let { score ->
                            item {
                                FilterBadge(label = "$score ⭐ Closeness") { viewModel.selectedClosenessFilter.value = null }
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.clearAllFilters() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Clear Filters",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // --- CONTACT LIST ---
            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (contacts.isEmpty()) "No contacts registered yet" else "No matching relations found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (contacts.isEmpty()) "Tap the + button to build your personal relationship graph." else "Try adjusting your filters or query text.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 32.dp),
                            align = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ContactItemCard(
                            contact = contact,
                            onClick = {
                                viewModel.selectContact(contact.id)
                                onContactClick(contact.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // --- FILTER BOTTOM SHEET ---
    if (showFilterSheet) {
        AlertDialog(
            onDismissRequest = { showFilterSheet = false },
            confirmButton = {
                TextButton(onClick = { showFilterSheet = false }) {
                    Text("Done")
                }
            },
            title = {
                Text("Filter Relationship Ledger", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    // Overdue Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pending Follow-ups", fontWeight = FontWeight.SemiBold)
                            Text("Show only contacts that are overdue", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = overdueOnly,
                            onCheckedChange = { viewModel.showOnlyOverdue.value = it }
                        )
                    }

                    Divider()

                    // Closeness Filter
                    Column {
                        Text("Closeness Strength", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..5).forEach { score ->
                                val isSelected = activeCloseness == score
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.selectedClosenessFilter.value = if (isSelected) null else score
                                    },
                                    label = { Text("$score ⭐") }
                                )
                            }
                        }
                    }

                    if (tags.isNotEmpty()) {
                        Divider()
                        // Tags Filter
                        Column {
                            Text("Filter by Tag", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                tags.forEach { tag ->
                                    val isSelected = activeTag == tag
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.selectedFilterTag.value = if (isSelected) null else tag
                                        },
                                        label = { Text("#$tag") }
                                    )
                                }
                            }
                        }
                    }

                    if (locations.isNotEmpty()) {
                        Divider()
                        // Location Filter
                        Column {
                            Text("Filter by Geography", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                locations.forEach { location ->
                                    val isSelected = activeLocation == location
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.selectedLocationFilter.value = if (isSelected) null else location
                                        },
                                        label = { Text("📍 $location") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FilterBadge(
    label: String,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

@Composable
fun ContactItemCard(
    contact: Contact,
    onClick: () -> Unit
) {
    val score = contact.calculateStrengthScore()
    val isOverdue = contact.isFollowUpOverdue()

    val strengthColor = if (score >= 7.5) {
        IntensityStrong
    } else if (score >= 4.5) {
        IntensityMedium
    } else {
        IntensityWeak
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("contact_item_${contact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circle with initials
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(strengthColor.copy(alpha = 0.15f))
            ) {
                Text(
                    text = contact.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = strengthColor
                    )
                )

                // Miniature Strength bar on edge or just dot
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(strengthColor)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text layout
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Overdue alert badge
                    if (isOverdue) {
                        Surface(
                            color = IntensityWeak.copy(alpha = 0.15f),
                            contentColor = IntensityWeak,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "OVERDUE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (contact.company.isNotBlank() || contact.position.isNotBlank()) {
                    val jobText = listOf(contact.position, contact.company)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    Text(
                        text = jobText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Tags Summary Row
                if (contact.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    ) {
                        contact.tags.take(3).forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (contact.tags.size > 3) {
                            Text(
                                text = "+${contact.tags.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }

                if (contact.lastInteractionSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💬 ${contact.lastInteractionSummary}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            )
        }
    }
}
