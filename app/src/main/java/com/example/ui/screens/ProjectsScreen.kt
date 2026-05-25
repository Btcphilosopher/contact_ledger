package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.model.Project
import com.example.ui.theme.IntensityMedium
import com.example.ui.theme.IntensityStrong
import com.example.ui.theme.IntensityWeak
import com.example.ui.viewmodel.ContactLedgerViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ContactLedgerViewModel,
    onContactClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.allProjects.collectAsState()
    val selectedProjectId by viewModel.selectedProjectId.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val projectContacts by viewModel.selectedProjectContacts.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()

    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showLinkContactDialog by remember { mutableStateOf(false) }

    Row(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // --- LEFT COLUMN: PROJECTS SIDEBAR LIST ---
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.2f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Projects & Deals",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = { showAddProjectDialog = true },
                    modifier = Modifier.size(28.dp).testTag("add_project_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Project", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            if (projects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No active projects. Tap + to create one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        align = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(projects, key = { it.id }) { proj ->
                        val isSelected = selectedProjectId == proj.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                                .clickable { viewModel.selectProject(proj.id) }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (proj.status == "Active") Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proj.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = proj.status,
                                    fontSize = 10.sp,
                                    color = if (proj.status == "Active") IntensityStrong else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }

        VerticalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        // --- RIGHT COLUMN: DETAILED NODE VIEW ---
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.8f)
                .padding(16.dp)
        ) {
            val currentProject = selectedProject

            if (currentProject == null) {
                // Empty details placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Select a project link diagram to explore connected partners & collaborators.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            align = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                showAddProjectDialogFlow(showAddProjectDialog, viewModel, onDismiss = { showAddProjectDialog = false })
                return@Column
            }

            // High-Resolution details board
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        currentProject.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (currentProject.status == "Active") IntensityStrong.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (currentProject.status == "Active") IntensityStrong else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = currentProject.status.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.deleteProject(currentProject.id) {
                            Toast.makeText(context, "Project node deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("delete_project_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (currentProject.description.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = currentProject.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Connected nodes row header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Connected Contacts Graph",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = { showLinkContactDialog = true },
                    modifier = Modifier.size(28.dp).testTag("link_contact_button")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Add Contact Node", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

            if (projectContacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No contacts linked to this project node yet. Link builders, contractors, or suppliers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        align = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(projectContacts, key = { it.id }) { contact ->
                        val strength = contact.calculateStrengthScore()
                        val col = if (strength >= 7.5) IntensityStrong else if (strength >= 4.5) IntensityMedium else IntensityWeak
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).clickable { onContactClick(contact.id) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(col.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            contact.name.take(2).uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = col
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            contact.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (contact.position.isNotBlank()) {
                                            Text(
                                                contact.position,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.unlinkContactFromProject(currentProject.id, contact.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.LinkOff, contentDescription = "Unlink", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS CONTAINER ---
    showAddProjectDialogFlow(showAddProjectDialog, viewModel, onDismiss = { showAddProjectDialog = false })

    if (showLinkContactDialog && selectedProject != null) {
        val nonAssociatedContacts = allContacts.filter { c -> projectContacts.none { pc -> pc.id == c.id } }

        AlertDialog(
            onDismissRequest = { showLinkContactDialog = false },
            confirmButton = {
                TextButton(onClick = { showLinkContactDialog = false }) {
                    Text("Close")
                }
            },
            title = { Text("Link Contact Node") },
            text = {
                if (nonAssociatedContacts.isEmpty()) {
                    Text("All of your database contacts have already been mapped to this project graph.")
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())
                    ) {
                        nonAssociatedContacts.forEach { contact ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.linkContactToProject(selectedProject!!.id, contact.id)
                                        showLinkContactDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.Bold)
                                        if (contact.company.isNotBlank()) {
                                            Text(contact.company, style = MaterialTheme.typography.bodySmall)
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
}

@Composable
fun showAddProjectDialogFlow(
    show: Boolean,
    viewModel: ContactLedgerViewModel,
    onDismiss: () -> Unit
) {
    if (show) {
        val context = LocalContext.current
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Active") }
        var isStatusMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveProject(
                                Project(
                                    name = name.trim(),
                                    description = description.trim(),
                                    status = status
                                )
                            )
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please key in a name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_project_confirm")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            title = { Text("Create Project Node") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project / Deal Name *") },
                        placeholder = { Text("e.g. House Renovation Project") },
                        modifier = Modifier.fillMaxWidth().testTag("project_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Objective") },
                        placeholder = { Text("e.g. Full rewiring and brick layout specs") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = status,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Project Status") },
                            trailingIcon = {
                                IconButton(onClick = { isStatusMenuExpanded = !isStatusMenuExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = isStatusMenuExpanded,
                            onDismissRequest = { isStatusMenuExpanded = false }
                        ) {
                            listOf("Active", "On Hold", "Completed").forEach { state ->
                                DropdownMenuItem(
                                    text = { Text(state) },
                                    onClick = {
                                        status = state
                                        isStatusMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
