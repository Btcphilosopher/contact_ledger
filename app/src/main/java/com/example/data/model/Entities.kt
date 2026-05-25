package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val position: String = "",
    val location: String = "", // e.g., "London", "San Francisco"
    val howWeMet: String = "",
    val closenessScore: Int = 3, // 1 (Weak tie) to 5 (Strong tie)
    val followUpIntervalDays: Int = 90, // 0 means no follow up, or 30, 90, 180
    val lastInteractionDate: Long? = null, // timestamp in ms
    val lastInteractionSummary: String = "",
    val notes: String = "",
    val socialLinkedin: String = "",
    val socialTwitter: String = "",
    val socialWeb: String = "",
    val tagsString: String = "", // comma separated tags
    val createdAt: Long = System.currentTimeMillis()
) {
    val tags: List<String>
        get() = if (tagsString.isBlank()) {
            emptyList()
        } else {
            tagsString.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

    fun isFollowUpOverdue(): Boolean {
        if (followUpIntervalDays <= 0) return false
        val baseline = lastInteractionDate ?: createdAt
        val millisecondsInDay = 24 * 60 * 60 * 1000L
        val daysPassed = (System.currentTimeMillis() - baseline) / millisecondsInDay
        return daysPassed >= followUpIntervalDays
    }

    fun getDaysUntilFollowUp(): Int {
        if (followUpIntervalDays <= 0) return 999
        val baseline = lastInteractionDate ?: createdAt
        val millisecondsInDay = 24 * 60 * 60 * 1000L
        val daysPassed = (System.currentTimeMillis() - baseline) / millisecondsInDay
        return (followUpIntervalDays - daysPassed).toInt()
    }

    fun formattedLastInteraction(): String {
        val date = lastInteractionDate ?: return "No interactions registered"
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(date))
    }

    // Dynamic relationship strength based on proximity, closeness, and recency of interaction
    fun calculateStrengthScore(): Double {
        // Base weight on manual closeness score (trust level)
        val trustWeight = closenessScore.toDouble() / 5.0 // Range [0.2, 1.0]

        // Recency weight (last 30 days is 1.0, decaying to 0 over 180 days)
        val daysSinceLimit = 180
        val baseline = lastInteractionDate ?: createdAt
        val millisecondsInDay = 24 * 60 * 60 * 1000L
        val daysPassed = ((System.currentTimeMillis() - baseline) / millisecondsInDay).coerceAtLeast(0)
        
        val recencyWeight = if (daysPassed <= 30) {
            1.0
        } else if (daysPassed >= daysSinceLimit) {
            0.1
        } else {
            1.0 - (daysPassed - 30).toDouble() / (daysSinceLimit - 30).toDouble()
        }

        // Return a visual scale metric [0 to 10]
        return (trustWeight * 6.0 + recencyWeight * 4.0).coerceIn(1.0, 10.0)
    }
}

@Entity(tableName = "interactions")
data class Interaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactId: Int,
    val type: String, // "Call", "Message", "Meeting", "Note", "Social"
    val summary: String,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    fun formattedDate(): String {
        val sdf = SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault())
        return sdf.format(Date(date))
    }
}

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val status: String = "Active", // "Active", "Completed", "On Hold"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_contact_links", primaryKeys = ["projectId", "contactId"])
data class ProjectContactLink(
    val projectId: Int,
    val contactId: Int
)
