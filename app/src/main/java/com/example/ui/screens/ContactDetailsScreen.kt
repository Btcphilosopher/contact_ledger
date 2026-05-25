package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.model.Interaction
import com.example.data.model.Project
import com.example.ui.theme.IntensityMedium
import com.example.ui.theme.IntensityStrong
import com.example.ui.theme.IntensityWeak
import com.example.ui.viewmodel.ContactLedgerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
    contactId: Int,
    viewModel: ContactLedgerViewModel,
    onEditClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Bind flows
    val contact by viewModel.selectedContact.collectAsState()
    val interactions by viewModel.selectedContactInteractions.collectAsState()
    val linkedProjects by viewModel.selectedContactProjects.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()

    var showLogDialog by remember { mutableStateOf(false) }
    var showLinkProjectDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Re-verify selection
    LaunchedEffect(contactId) {
        viewModel.selectContact(contactId)
    }

    val currentContact = contact

    if (currentContact == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Intelligence") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEditClick(currentContact.id) },
                        modifier = Modifier.testTag("edit_contact_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Contact")
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_contact_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Contact", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- HEADER INFO SECTION ---
            item {
                ContactHeaderSection(contact = currentContact, context = context)
            }

            // --- QUICK ACTION HUB (INTENTS) ---
            item {
                QuickActionHub(contact = currentContact, context = context)
            }

            // --- RELATIONSHIP HEALTH PANEL ---
            item {
                RelationshipHealthPanel(contact = currentContact)
            }

            // --- HOW WE MET / MEMORY BOX ---
            item {
                MemoryVaultSection(contact = currentContact)
            }

            // --- LINKED PROJECTS HUB ---
            item {
                LinkedProjectsSection(
                    projects = linkedProjects,
                    onLinkClick = { showLinkProjectDialog = true },
                    onUnlinkClick = { projId -> viewModel.unlinkContactFromProject(projId, currentContact.id) }
                )
            }

            // --- TIMELINE CONTROLS HEADER ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interaction Timeline",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(
                        onClick = { showLogDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("log_interaction_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log", fontSize = 12.sp)
                    }
                }
            }

            // --- TIMELINE ITEMS ---
            if (interactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No interaction logs recorded. Click 'Log' to register your first meeting or call.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            align = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                items(interactions, key = { it.id }) { interaction ->
                    TimelineItem(
                        interaction = interaction,
                        onDeleteClick = { viewModel.deleteInteraction(interaction) }
                    )
                }
            }
        }
    }

    // --- DIALOG: LOG NEW INTERACTION ---
    if (showLogDialog) {
        var logType by remember { mutableStateOf("Meeting") }
        var logSummary by remember { mutableStateOf("") }
        var logNotes by remember { mutableStateOf("") }
        var isExpandedType by remember { mutableStateOf(false) }

        val interactionTypes = listOf("Meeting", "Call", "Message", "Email", "Note")

        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (logSummary.isNotBlank()) {
                            viewModel.logInteraction(
                                contactId = currentContact.id,
                                type = logType,
                                summary = logSummary,
                                notes = logNotes,
                                date = System.currentTimeMillis()
                            )
                            showLogDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a summary", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_log_confirm_button")
                ) {
                    Text("Save Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Log Interaction") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    // Type selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = logType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Interaction Type") },
                            trailingIcon = {
                                IconButton(onClick = { isExpandedType = !isExpandedType }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = isExpandedType,
                            onDismissRequest = { isExpandedType = false }
                        ) {
                            interactionTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        logType = type
                                        isExpandedType = false
                                    }
                                )
                            }
                        }
                    }

                    // Summary
                    OutlinedTextField(
                        value = logSummary,
                        onValueChange = { logSummary = it },
                        label = { Text("What did you talk about? (Summary)") },
                        placeholder = { Text("e.g. Chat about renovated kitchen spec") },
                        modifier = Modifier.fillMaxWidth().testTag("log_summary_input"),
                        singleLine = true
                    )

                    // Notes
                    OutlinedTextField(
                        value = logNotes,
                        onValueChange = { logNotes = it },
                        label = { Text("Additional Memory Details (Optional)") },
                        placeholder = { Text("e.g. He likes dark wood finish. Quoted £500.") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4
                    )
                }
            }
        )
    }

    // --- DIALOG: LINK PRODUCT/PROJECT ---
    if (showLinkProjectDialog) {
        val unassociatedProjects = allProjects.filter { p -> linkedProjects.none { lp -> lp.id == p.id } }

        AlertDialog(
            onDismissRequest = { showLinkProjectDialog = false },
            confirmButton = {
                TextButton(onClick = { showLinkProjectDialog = false }) {
                    Text("Close")
                }
            },
            title = { Text("Link Project Node") },
            text = {
                if (unassociatedProjects.isEmpty()) {
                    Text(
                        "No unlinked projects available. Create projects first from the Projects panel to build graph relationships.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())
                    ) {
                        unassociatedProjects.forEach { project ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.linkContactToProject(project.id, currentContact.id)
                                        showLinkProjectDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(project.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        if (project.description.isNotBlank()) {
                                            Text(project.description, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Icon(Icons.Default.Link, contentDescription = "Link")
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // --- DIALOG: ACTIONS FOR DELETION ---
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to permanently delete ${currentContact.name}? All associated interaction logs and linkages will be purged.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteContact(currentContact.id, onDeleted = onBackClick)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ContactHeaderSection(contact: Contact, context: android.content.Context) {
    val score = contact.calculateStrengthScore()
    val strengthColor = if (score >= 7.5) IntensityStrong else if (score >= 4.5) IntensityMedium else IntensityWeak

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Initial Avatar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(strengthColor.copy(alpha = 0.15f))
        ) {
            Text(
                text = contact.name.take(2).uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = strengthColor,
                    fontSize = 32.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = contact.name,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 24.sp),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (contact.position.isNotBlank() || contact.company.isNotBlank()) {
            val roleText = listOf(contact.position, contact.company)
                .filter { it.isNotBlank() }
                .joinToString(" at ")
            Text(
                text = roleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (contact.location.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable {
                        // Highlight Location logic
                        Toast.makeText(context, "Location tagged: ${contact.location}", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = contact.location,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Horizontal tags row
        if (contact.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                contact.tags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionHub(contact: Contact, context: android.content.Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val buttonModifier = Modifier.size(52.dp)
        
        // --- Dial Call intent ---
        IconButton(
            onClick = {
                if (contact.phone.isNotBlank()) {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                        context.startActivity(dialIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot dial standard calls", Toast.LENGTH_SHORT).show()
                    }
                } else Toast.makeText(context, "No phone details stored", Toast.LENGTH_SHORT).show()
            },
            enabled = contact.phone.isNotBlank(),
            modifier = buttonModifier.background(
                color = if (contact.phone.isNotBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
        ) {
            Icon(Icons.Default.Phone, contentDescription = "Call", tint = if (contact.phone.isNotBlank()) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray)
        }

        // --- Mail compose intent ---
        IconButton(
            onClick = {
                if (contact.email.isNotBlank()) {
                    try {
                        val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))
                        context.startActivity(mailIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open mail client", Toast.LENGTH_SHORT).show()
                    }
                } else Toast.makeText(context, "No email details stored", Toast.LENGTH_SHORT).show()
            },
            enabled = contact.email.isNotBlank(),
            modifier = buttonModifier.background(
                color = if (contact.email.isNotBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
        ) {
            Icon(Icons.Default.Email, contentDescription = "Email", tint = if (contact.email.isNotBlank()) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray)
        }

        // --- Twitter / Web intents ---
        val hasTwitter = contact.socialTwitter.isNotBlank()
        IconButton(
            onClick = {
                if (hasTwitter) {
                    val twitterUrl = if (contact.socialTwitter.startsWith("http")) contact.socialTwitter else "https://twitter.com/${contact.socialTwitter}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(twitterUrl)))
                } else Toast.makeText(context, "No Twitter handle stored", Toast.LENGTH_SHORT).show()
            },
            enabled = hasTwitter,
            modifier = buttonModifier.background(
                color = if (hasTwitter) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
        ) {
            Icon(Icons.Default.Share, contentDescription = "Twitter", tint = if (hasTwitter) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray)
        }

        // --- LinkedIn intent ---
        val hasLinkedin = contact.socialLinkedin.isNotBlank()
        IconButton(
            onClick = {
                if (hasLinkedin) {
                    val liUrl = if (contact.socialLinkedin.startsWith("http")) contact.socialLinkedin else "https://www.linkedin.com/in/${contact.socialLinkedin}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(liUrl)))
                } else Toast.makeText(context, "No LinkedIn profile stored", Toast.LENGTH_SHORT).show()
            },
            enabled = hasLinkedin,
            modifier = buttonModifier.background(
                color = if (hasLinkedin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
        ) {
            Icon(Icons.Default.Business, contentDescription = "LinkedIn", tint = if (hasLinkedin) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray)
        }
    }
}

@Composable
fun RelationshipHealthPanel(contact: Contact) {
    val score = contact.calculateStrengthScore()
    val isOverdue = contact.isFollowUpOverdue()
    val daysUntil = contact.getDaysUntilFollowUp()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Relationship Intelligence Indicators",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar rating metric
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Relationship Weight", fontWeight = FontWeight.SemiBold)
                Text(
                    text = String.format(Locale.getDefault(), "%.1f / 10", score),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (score / 10.0f).coerceIn(0.0f, 1.0f).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(12.dp))

            // Reconnection Schedule Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Reconnection Alert Engine", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (contact.followUpIntervalDays > 0) "Every ${contact.followUpIntervalDays} days" else "Manual logs only (Automations Off)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                if (contact.followUpIntervalDays <= 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "STABLE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (isOverdue) {
                    Surface(
                        color = IntensityWeak.copy(alpha = 0.15f),
                        contentColor = IntensityWeak,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "OVERDUE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = IntensityStrong.copy(alpha = 0.15f),
                        contentColor = IntensityStrong,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "ACTIVE (In $daysUntil days)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                Text(
                    text = if (contact.lastInteractionDate == null) {
                        "Never interacted synthetically before."
                    } else {
                        "Last logged connection: ${contact.formattedLastInteraction()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MemoryVaultSection(contact: Contact) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Relationship Context Memory Vault",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // "How we met"
            if (contact.howWeMet.isNotBlank()) {
                Text("How We Met Context", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = contact.howWeMet,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Text(
                    "No 'How We Met' context captured. Edit this profile to specify where you met this node.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            if (contact.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                Spacer(modifier = Modifier.height(10.dp))
                Text("General Context Intelligence", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = contact.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun LinkedProjectsSection(
    projects: List<Project>,
    onLinkClick: () -> Unit,
    onUnlinkClick: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Linked Professional Projects Nodes",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                IconButton(onClick = onLinkClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Link, contentDescription = "Link Project", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (projects.isEmpty()) {
                Text(
                    "Not linked to any projects or business pipelines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    projects.forEach { project ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(project.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = "Status: ${project.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (project.status == "Active") IntensityStrong else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(onClick = { onUnlinkClick(project.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.LinkOff, contentDescription = "Unlink", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineItem(
    interaction: Interaction,
    onDeleteClick: () -> Unit
) {
    val icon = when (interaction.type.lowercase()) {
        "call" -> Icons.Default.Phone
        "email" -> Icons.Default.Email
        "message" -> Icons.Default.Message
        "note" -> Icons.Default.Edit
        else -> Icons.Default.Timeline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        // Decorative Timeline bullet line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
            // Vertical connection dash
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Log detail box
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = interaction.formattedDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Log", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = interaction.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (interaction.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = interaction.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
