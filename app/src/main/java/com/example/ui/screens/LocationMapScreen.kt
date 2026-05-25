package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.text.drawText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.ui.theme.IntensityMedium
import com.example.ui.theme.IntensityStrong
import com.example.ui.theme.IntensityWeak
import com.example.ui.viewmodel.ContactLedgerViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocationMapScreen(
    viewModel: ContactLedgerViewModel,
    onContactClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contacts by viewModel.allContacts.collectAsState()
    val simulatedCity by viewModel.simulatedLocation.collectAsState()
    
    // Extrapolate available locations to offer simulation drop points
    val storedCities by viewModel.allLocationsList.collectAsState()
    val defaultCities = listOf("London", "Manchester", "New York", "San Francisco", "Tokyo", "Berlin")
    val allSimCities = remember(storedCities) {
        (defaultCities + storedCities).distinct().filter { it.isNotBlank() }.sorted()
    }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Nearby relations criteria: matches location ignoring case
    val nearbyContacts = remember(contacts, simulatedCity) {
        contacts.filter { it.location.equals(simulatedCity, ignoreCase = false) || it.location.contains(simulatedCity, ignoreCase = true) }
    }

    // Coordinates mapping for Canvas hits
    var nodeCoordinates = remember { mutableStateMapOf<Int, Offset>() }
    var clickedContactId by remember { mutableStateOf<Int?>(null) }
    val selectedPreviewContact = remember(clickedContactId, contacts) {
        contacts.find { it.id == clickedContactId }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- LOCATIONS CONTROL TOP BAR ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp, horizontal = 20.dp)
        ) {
            Text(
                "Location Intelligence Map",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
            Text(
                "Track professional proximity clusters offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Simulation Dropdown Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { isDropdownExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulation_selector_box"),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simulating coordinates: $simulatedCity",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    allSimCities.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city) },
                            onClick = {
                                viewModel.simulatedLocation.value = city
                                isDropdownExpanded = false
                                clickedContactId = null // Clear preview on teleport
                            }
                        )
                    }
                }
            }
        }

        // --- MAP GRAPH PANEL CANVAS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
        ) {
            val textMeasurer = rememberTextMeasurer()
            val textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            // Responsive concentric orbits calculations
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(nearbyContacts) {
                        detectTapGestures { offset ->
                            // Hit test: search for node coordinate clicked
                            val matchedId = nodeCoordinates.entries.find { (_, crd) ->
                                val distSq = (crd.x - offset.x) * (crd.x - offset.x) + (crd.y - offset.y) * (crd.y - offset.y)
                                distSq <= 30 * 30 // Approx 30px node hit box radius
                            }?.key
                            clickedContactId = matchedId
                        }
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.width.coerceAtMost(size.height) / 2.3f

                // 1. Draw coordinate grids
                val gridStroke = Stroke(width = 1f, pathEffect = null)
                val gridColor = Color.LightGray.copy(alpha = 0.25f)
                drawLine(gridColor, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)
                drawLine(gridColor, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)

                // 2. Draw orbits
                drawCircle(gridColor, radius = radius * 0.4f, center = Offset(cx, cy), style = Stroke(width = 2f))
                drawCircle(gridColor, radius = radius * 0.75f, center = Offset(cx, cy), style = Stroke(width = 2f))
                drawCircle(gridColor, radius = radius, center = Offset(cx, cy), style = Stroke(width = 2f))

                // 3. Center Position Pulse "You"
                drawCircle(Color(0xFF818CF8).copy(alpha = 0.15f), radius = 36f, center = Offset(cx, cy))
                drawCircle(Color(0xFF818CF8), radius = 10f, center = Offset(cx, cy))

                // 4. Draw orbital contacts
                nodeCoordinates.clear()
                val nodeRadius = 30f

                nearbyContacts.forEachIndexed { idx, contact ->
                    val closeness = contact.closenessScore
                    // Distance inverse proportional to strength/closeness
                    val orbitFactor = when (closeness) {
                        5 -> 0.4f
                        4, 3 -> 0.75f
                        else -> 1.0f
                    }
                    val distance = radius * orbitFactor
                    // Spread points evenly by angle calculations
                    val angleDeg = (360f / nearbyContacts.size) * idx
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val nx = cx + (distance * cos(angleRad)).toFloat()
                    val ny = cy + (distance * sin(angleRad)).toFloat()

                    nodeCoordinates[contact.id] = Offset(nx, ny)

                    val strength = contact.calculateStrengthScore()
                    val colorToken = if (strength >= 7.5) IntensityStrong else if (strength >= 4.5) IntensityMedium else IntensityWeak

                    // Node circles
                    drawCircle(colorToken.copy(alpha = 0.12f), radius = nodeRadius * 1.6f, center = Offset(nx, ny))
                    drawCircle(colorToken, radius = nodeRadius, center = Offset(nx, ny))
                    
                    // Draw contact initial text
                    val label = contact.name.take(1).uppercase()
                    val measureResult = textMeasurer.measure(label, textStyle.copy(color = Color.White))
                    val textOffset = Offset(
                        nx - measureResult.size.width / 2f,
                        ny - measureResult.size.height / 2f
                    )
                    drawText(measureResult, textOffset)
                }
            }

            // Interactive popup modal overlays
            AnimatedVisibility(
                visible = selectedPreviewContact != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
            ) {
                selectedPreviewContact?.let { contact ->
                    val score = contact.calculateStrengthScore()
                    val col = if (score >= 7.5) IntensityStrong else if (score >= 4.5) IntensityMedium else IntensityWeak
                    
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectContact(contact.id)
                                onContactClick(contact.id)
                            }
                            .testTag("node_map_preview")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(col.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    contact.name.take(2).uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = col,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    contact.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    listOf(contact.position, contact.company).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.selectContact(contact.id)
                                    onContactClick(contact.id)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Navigate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- LOWER VICINITY LIST ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Contacts (${nearbyContacts.size})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Currently in $simulatedCity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

            if (nearbyContacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No stored relationships listed in $simulatedCity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            align = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(nearbyContacts, key = { it.id }) { contact ->
                        val strength = contact.calculateStrengthScore()
                        val colorToken = if (strength >= 7.5) IntensityStrong else if (strength >= 4.5) IntensityMedium else IntensityWeak
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .clickable {
                                    viewModel.selectContact(contact.id)
                                    onContactClick(contact.id)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(colorToken)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        contact.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (contact.company.isNotBlank()) {
                                        Text(
                                            contact.company,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Catchup overdue?",
                                fontSize = 11.sp,
                                color = if (contact.isFollowUpOverdue()) Color.Red else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
