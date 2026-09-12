package com.lifemate.domain

import com.lifemate.database.SocialPost
import java.time.LocalDate

/** Offline, editable writing templates—not AI, never an automatic Facebook publisher. */
object PostTemplates {
    val types = listOf("Normal Post","Birthday Post","Motivation","Daily Routine","Achievement","Mission Completed","Quote","Celebration","Announcement","Personal Memory")
    val tones = listOf("Short","Friendly","Emotional","Professional")
    fun caption(post: SocialPost, tone: String = "Short", variation: Int = 0): String {
        val topic = post.topic.trim()
        val name = post.person.trim()
        val heading = when(post.type) {
            "Birthday Post" -> "Happy birthday${if(name.isBlank()) "" else ", $name"}!"
            "Motivation" -> if(variation%2==0) "One day. One step. One goal." else "Small steps are still steps forward."
            "Daily Routine" -> "Today's focus${if(topic.isBlank()) "" else ": $topic"}"
            "Achievement" -> "A milestone worth celebrating${if(topic.isBlank()) "." else ": $topic"}"
            "Mission Completed" -> "Mission completed${if(topic.isBlank()) "!" else ": $topic"}"
            "Quote" -> topic.ifBlank { "Make room for what matters." }
            "Celebration" -> "Here's to ${topic.ifBlank { "the little wins" }}!"
            "Announcement" -> topic.ifBlank { "A little announcement" }
            "Personal Memory" -> "A moment to remember${if(topic.isBlank()) "." else ": $topic"}"
            else -> topic.ifBlank { "A little moment from today." }
        }
        val extra = when(tone) {
            "Emotional" -> if(post.type=="Birthday Post") "So grateful for the memories we share. Wishing you love in this new chapter." else "Grateful for this chapter and the people who make it meaningful."
            "Professional" -> if(post.type=="Mission Completed" || post.type=="Achievement") "A meaningful step forward. Looking ahead to continued progress." else "Sharing a moment of progress and perspective."
            "Friendly" -> if(variation%2==0) "A little joy, shared with you." else "Here's to more moments like this."
            else -> ""
        }
        return listOf(heading,post.message.trim(),extra,if(post.type!="Birthday Post") name else "",post.date).filter { it.isNotBlank() }.joinToString("\n\n").take(4000)
    }
    fun graphic(post: SocialPost): String {
        val title = when(post.type) {
            "Birthday Post" -> "Happy Birthday"
            "Achievement","Mission Completed" -> "Mission Completed"
            "Motivation" -> "One Day\nOne Step\nOne Goal"
            "Personal Memory" -> "A Moment To Remember"
            else -> post.topic.ifBlank { post.type }
        }
        return listOf(title,post.person,post.message.ifBlank { if(post.type in setOf("Achievement","Mission Completed")) post.topic else "" },post.date).filter { it.isNotBlank() }.joinToString("\n\n").take(600)
    }
    fun validate(post: SocialPost) {
        require(post.type in types) { "Choose a post type." }
        require(post.topic.length<=160 && post.message.length<=600 && post.person.length<=100 && post.caption.length<=4000) { "Keep the post within the displayed limits." }
        require(post.topic.isNotBlank() || post.message.isNotBlank() || post.caption.isNotBlank()) { "Add a topic or create a caption." }
        if(post.date.isNotBlank()) LocalDate.parse(post.date)
    }
}
