package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Contact
import com.example.data.model.Interaction
import com.example.data.model.Project
import com.example.data.model.ProjectContactLink
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    // --- CONTACTS ---
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactById(id: Int): Flow<Contact?>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactByIdSuspended(id: Int): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Int)


    // --- INTERACTIONS ---
    @Query("SELECT * FROM interactions WHERE contactId = :contactId ORDER BY date DESC")
    fun getInteractionsForContact(contactId: Int): Flow<List<Interaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: Interaction): Long

    @Delete
    suspend fun deleteInteraction(interaction: Interaction)

    @Query("DELETE FROM interactions WHERE contactId = :contactId")
    suspend fun deleteInteractionsForContact(contactId: Int)


    // --- PROJECTS / DEALS ---
    @Query("SELECT * FROM projects ORDER BY name ASC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Int): Flow<Project?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)


    // --- CROSS REFERENCE LINKS ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProjectContactLink(link: ProjectContactLink)

    @Query("DELETE FROM project_contact_links WHERE projectId = :projectId AND contactId = :contactId")
    suspend fun deleteProjectContactLink(projectId: Int, contactId: Int)

    @Query("DELETE FROM project_contact_links WHERE contactId = :contactId")
    suspend fun deleteProjectLinksForContact(contactId: Int)

    @Query("DELETE FROM project_contact_links WHERE projectId = :projectId")
    suspend fun deleteContactLinksForProject(projectId: Int)

    @Query("""
        SELECT * FROM contacts 
        INNER JOIN project_contact_links ON contacts.id = project_contact_links.contactId 
        WHERE project_contact_links.projectId = :projectId
        ORDER BY contacts.name ASC
    """)
    fun getContactsForProject(projectId: Int): Flow<List<Contact>>

    @Query("""
        SELECT * FROM projects 
        INNER JOIN project_contact_links ON projects.id = project_contact_links.projectId 
        WHERE project_contact_links.contactId = :contactId
        ORDER BY projects.name ASC
    """)
    fun getProjectsForContact(contactId: Int): Flow<List<Project>>
}
