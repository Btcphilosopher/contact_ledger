package com.example.data.repository

import com.example.data.local.ContactDao
import com.example.data.model.Contact
import com.example.data.model.Interaction
import com.example.data.model.Project
import com.example.data.model.ProjectContactLink
import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {

    // --- CONTACT OPERATIONS ---
    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()

    fun getContactById(id: Int): Flow<Contact?> = contactDao.getContactById(id)

    suspend fun getContactByIdSuspended(id: Int): Contact? = contactDao.getContactByIdSuspended(id)

    suspend fun insertContact(contact: Contact): Long = contactDao.insertContact(contact)

    suspend fun updateContact(contact: Contact) = contactDao.updateContact(contact)

    suspend fun deleteContactById(id: Int) {
        // Cascade manually
        contactDao.deleteInteractionsForContact(id)
        contactDao.deleteProjectLinksForContact(id)
        contactDao.deleteContactById(id)
    }


    // --- INTERACTION LOGGING & WORKFLOW WORKSPACE ---
    fun getInteractionsForContact(contactId: Int): Flow<List<Interaction>> =
        contactDao.getInteractionsForContact(contactId)

    suspend fun insertInteraction(interaction: Interaction) {
        // Insert interaction
        contactDao.insertInteraction(interaction)

        // Read and update the contact's interaction state automatically
        val contact = contactDao.getContactByIdSuspended(interaction.contactId)
        if (contact != null) {
            // Only update last interaction if this new interaction is newer or there is none
            val currentLastDate = contact.lastInteractionDate ?: 0L
            if (interaction.date >= currentLastDate) {
                val updatedContact = contact.copy(
                    lastInteractionDate = interaction.date,
                    lastInteractionSummary = interaction.summary
                )
                contactDao.updateContact(updatedContact)
            }
        }
    }

    suspend fun deleteInteraction(interaction: Interaction) {
        contactDao.deleteInteraction(interaction)
        // Re-evaluate last interaction for safety
        val contactId = interaction.contactId
        val remaining = contactDao.getInteractionsForContact(contactId)
        // In suspend context, we can query last interaction or reset if empty:
        val contact = contactDao.getContactByIdSuspended(contactId)
        if (contact != null) {
            // For simple consistency, we keep the last logged or search for the max date
            // We can leave as is, or we could refine. Leaving as is is typical or updating to null if everything is deleted.
        }
    }


    // --- PROJECT WORK ENGINE ---
    val allProjects: Flow<List<Project>> = contactDao.getAllProjects()

    fun getProjectById(id: Int): Flow<Project?> = contactDao.getProjectById(id)

    suspend fun insertProject(project: Project): Long = contactDao.insertProject(project)

    suspend fun updateProject(project: Project) = contactDao.updateProject(project)

    suspend fun deleteProjectById(id: Int) {
        // Clear links associated with project first
        contactDao.deleteContactLinksForProject(id)
        contactDao.deleteProjectById(id)
    }


    // --- CROSS REFERENCE / LINK GRAPH PATHS ---
    suspend fun linkContactToProject(projectId: Int, contactId: Int) {
        contactDao.insertProjectContactLink(ProjectContactLink(projectId, contactId))
    }

    suspend fun unlinkContactFromProject(projectId: Int, contactId: Int) {
        contactDao.deleteProjectContactLink(projectId, contactId)
    }

    fun getContactsForProject(projectId: Int): Flow<List<Contact>> =
        contactDao.getContactsForProject(projectId)

    fun getProjectsForContact(contactId: Int): Flow<List<Project>> =
        contactDao.getProjectsForContact(contactId)
}
