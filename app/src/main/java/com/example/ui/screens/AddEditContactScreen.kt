package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.ui.viewmodel.ContactLedgerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditContactScreen(
    contactId: Int, // 0 for new contact, or id for editing
    viewModel: ContactLedgerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contactState by viewModel.selectedContact.collectAsState()

    // Form Field States
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var howMet by remember { mutableStateOf("") }
    var closenessScore by remember { mutableIntStateOf(3) }
    var followUpDays by remember { mutableIntStateOf(90) }
    var notes by remember { mutableStateOf("") }
    var linkedin by remember { mutableStateOf("") }
    var twitter by remember { mutableStateOf("") }
    var web by remember { mutableStateOf("") }
    var tagsString by remember { mutableStateOf("") }

    // Fetch and Populate editing records
    LaunchedEffect(contactId) {
        if (contactId > 0) {
            viewModel.selectContact(contactId)
        } else {
            viewModel.selectContact(null)
        }
    }

    LaunchedEffect(contactState, contactId) {
        val contact = contactState
        if (contact != null && contactId > 0) {
            name = contact.name
            phone = contact.phone
            email = contact.email
            company = contact.company
            position = contact.position
            location = contact.location
            howMet = contact.howWeMet
            closenessScore = contact.closenessScore
            followUpDays = contact.followUpIntervalDays
            notes = contact.notes
            linkedin = contact.socialLinkedin
            twitter = contact.socialTwitter
            web = contact.socialWeb
            tagsString = contact.tagsString
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (contactId > 0) "Refine Profile Detail" else "Map Core Contact") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Full Name is required.", Toast.LENGTH_SHORT).show()
                            } else {
                                val contactToSave = Contact(
                                    id = if (contactId > 0) contactId else 0,
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    company = company.trim(),
                                    position = position.trim(),
                                    location = location.trim(),
                                    howWeMet = howMet.trim(),
                                    closenessScore = closenessScore,
                                    followUpIntervalDays = followUpDays,
                                    notes = notes.trim(),
                                    socialLinkedin = linkedin.trim(),
                                    socialTwitter = twitter.trim(),
                                    socialWeb = web.trim(),
                                    tagsString = tagsString.trim()
                                )
                                viewModel.saveContact(contactToSave) {
                                    Toast.makeText(context, "Contact relationship secure.", Toast.LENGTH_SHORT).show()
                                    onBackClick()
                                }
                            }
                        },
                        modifier = Modifier.testTag("save_contact_button")
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Save")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "PRIMARY METADATA IDENTITY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                placeholder = { Text("e.g. Liam Smith") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Position / Role") },
                    placeholder = { Text("e.g. Lead Builder") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company / Brand") },
                    placeholder = { Text("e.g. Acme Building Co") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Geography / Base Location") },
                placeholder = { Text("e.g. London") },
                modifier = Modifier.fillMaxWidth().testTag("contact_location_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Divider()

            Text(
                "CHANNELS & NETWORKS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Telephone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = linkedin,
                onValueChange = { linkedin = it },
                label = { Text("LinkedIn Username / URL") },
                placeholder = { Text("e.g. liam-smith-renovations") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = twitter,
                    onValueChange = { twitter = it },
                    label = { Text("Twitter Handle") },
                    placeholder = { Text("e.g. @liamrenos") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = web,
                    onValueChange = { web = it },
                    label = { Text("Personal Website") },
                    placeholder = { Text("e.g. liamrenovations.com") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Divider()

            Text(
                "INTELLIGENCE GRAPH CLASSIFIERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = tagsString,
                onValueChange = { tagsString = it },
                label = { Text("Context Tags (Comma separated)") },
                placeholder = { Text("e.g. builder, contractor, london, family") },
                modifier = Modifier.fillMaxWidth().testTag("contact_tags_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Dynamic tags previews
            val currentTags = remember(tagsString) {
                if (tagsString.isBlank()) emptyList()
                else tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }

            if (currentTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    currentTags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Closeness Rating Sliders
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Calculated Closeness Strength: $closenessScore",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "1 = Weak tie / rare interact, 5 = Premium key relationship",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Slider(
                    value = closenessScore.toFloat(),
                    onValueChange = { closenessScore = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth().testTag("closeness_slider")
                )
            }

            // Reconnection Schedules
            Column {
                Text(
                    text = "Automatic Reconnection Cycle",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Alert tracker triggers warning if follow up isn't logged within limits.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val cycles = listOf(
                    "None" to 0,
                    "7 Days" to 7,
                    "30 Days" to 30,
                    "90 Days" to 90,
                    "180 Days" to 180
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    cycles.forEach { (label, days) ->
                        val isSelected = followUpDays == days
                        OutlinedButton(
                            onClick = { followUpDays = days },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                        ) {
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Divider()

            Text(
                "CONTEXTUAL MEMORIES & INITIAL LOGS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = howMet,
                onValueChange = { howMet = it },
                label = { Text("How We Met Context?") },
                placeholder = { Text("e.g. Met at Tech Crunch London meetup 2024") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("General Relationship Notes") },
                placeholder = { Text("e.g. Liam prefers text, has a dog, active builder.") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                singleLine = false,
                maxLines = 5,
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
