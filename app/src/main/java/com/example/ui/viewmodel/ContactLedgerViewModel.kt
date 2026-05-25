package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Contact
import com.example.data.model.Interaction
import com.example.data.model.Project
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactLedgerViewModel(private val repository: ContactRepository) : ViewModel() {

    // --- SEARCH / FILTER STATES ---
    val searchQuery = MutableStateFlow("")
    val selectedFilterTag = MutableStateFlow<String?>(null)
    val selectedLocationFilter = MutableStateFlow<String?>(null)
    val selectedClosenessFilter = MutableStateFlow<Int?>(null)
    val showOnlyOverdue = MutableStateFlow(false)

    // --- ACTIVE SELECTIONS FOR IN-DEPTH RECIPIENTS ---
    private val _selectedContactId = MutableStateFlow<Int?>(null)
    val selectedContactId = _selectedContactId.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<Int?>(null)
    val selectedProjectId = _selectedProjectId.asStateFlow()

    // --- RECOGNIZED GEOGRAPHY (SIMULATED GPS WORKSPACE) ---
    val simulatedLocation = MutableStateFlow("London") // Allows simulating current travel coordinates

    // --- DATA STREAM SEEDFLOWS ---
    val allContacts = repository.allContacts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allProjects = repository.allProjects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- REACTIVE MULTI-FILTER COMBINATOR ENGINE ---
    val filteredContacts: StateFlow<List<Contact>> = combine(
        allContacts,
        searchQuery,
        selectedFilterTag,
        selectedLocationFilter,
        selectedClosenessFilter,
        showOnlyOverdue
    ) { contacts, query, tag, location, closeness, overdue ->
        contacts.filter { contact ->
            val matchesQuery = query.isBlank() || 
                    contact.name.contains(query, ignoreCase = true) ||
                    contact.company.contains(query, ignoreCase = true) ||
                    contact.position.contains(query, ignoreCase = true) ||
                    contact.notes.contains(query, ignoreCase = true)
            
            val matchesTag = tag == null || contact.tags.any { it.equals(tag, ignoreCase = true) }
            val matchesLocation = location == null || contact.location.equals(location, ignoreCase = true)
            val matchesCloseness = closeness == null || contact.closenessScore == closeness
            val matchesOverdue = !overdue || contact.isFollowUpOverdue()

            matchesQuery && matchesTag && matchesLocation && matchesCloseness && matchesOverdue
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- AGGREGATE SUMMARY FEEDBACK FOR CHIPS ---
    val allTagsList: StateFlow<List<String>> = allContacts.map { contacts ->
        contacts.flatMap { it.tags }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLocationsList: StateFlow<List<String>> = allContacts.map { contacts ->
        contacts.map { it.location }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- ACTIVE BINDINGS (FLAT MAP PIPELINES) ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedContact: StateFlow<Contact?> = _selectedContactId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getContactById(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedContactInteractions: StateFlow<List<Interaction>> = _selectedContactId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getInteractionsForContact(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedContactProjects: StateFlow<List<Project>> = _selectedContactId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getProjectsForContact(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedProject: StateFlow<Project?> = _selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getProjectById(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedProjectContacts: StateFlow<List<Contact>> = _selectedProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getContactsForProject(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- WORK ENGINE ACTIONS ---
    fun selectContact(id: Int?) {
        _selectedContactId.value = id
    }

    fun selectProject(id: Int?) {
        _selectedProjectId.value = id
    }

    fun saveContact(contact: Contact, onSaved: (Contact) -> Unit = {}) {
        viewModelScope.launch {
            if (contact.id == 0) {
                val generatedId = repository.insertContact(contact)
                onSaved(contact.copy(id = generatedId.toInt()))
            } else {
                repository.updateContact(contact)
                onSaved(contact)
            }
        }
    }

    fun deleteContact(contactId: Int, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteContactById(contactId)
            if (_selectedContactId.value == contactId) {
                _selectedContactId.value = null
            }
            onDeleted()
        }
    }

    fun logInteraction(contactId: Int, type: String, summary: String, notes: String, date: Long) {
        viewModelScope.launch {
            val interaction = Interaction(
                contactId = contactId,
                type = type,
                summary = summary,
                notes = notes,
                date = date
            )
            repository.insertInteraction(interaction)
        }
    }

    fun deleteInteraction(interaction: Interaction) {
        viewModelScope.launch {
            repository.deleteInteraction(interaction)
        }
    }

    fun saveProject(project: Project, onSaved: (Project) -> Unit = {}) {
        viewModelScope.launch {
            if (project.id == 0) {
                val generatedId = repository.insertProject(project)
                onSaved(project.copy(id = generatedId.toInt()))
            } else {
                repository.updateProject(project)
                onSaved(project)
            }
        }
    }

    fun deleteProject(projectId: Int, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteProjectById(projectId)
            if (_selectedProjectId.value == projectId) {
                _selectedProjectId.value = null
            }
            onDeleted()
        }
    }

    fun linkContactToProject(projectId: Int, contactId: Int) {
        viewModelScope.launch {
            repository.linkContactToProject(projectId, contactId)
        }
    }

    fun unlinkContactFromProject(projectId: Int, contactId: Int) {
        viewModelScope.launch {
            repository.unlinkContactFromProject(projectId, contactId)
        }
    }

    fun clearAllFilters() {
        searchQuery.value = ""
        selectedFilterTag.value = null
        selectedLocationFilter.value = null
        selectedClosenessFilter.value = null
        showOnlyOverdue.value = false
    }
}

// Custom ViewModel Factory supporting Room database Injection
class ContactLedgerViewModelFactory(private val repository: ContactRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactLedgerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactLedgerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
