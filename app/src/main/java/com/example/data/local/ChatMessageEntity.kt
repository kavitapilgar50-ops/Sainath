package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.ChatMessage
import com.example.data.model.GroundingSource
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "chat_messages")
@TypeConverters(Converters::class)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "USER" or "ASSISTANT"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGrounded: Boolean = false,
    val searchQueries: List<String> = emptyList(),
    val sources: List<GroundingSource> = emptyList(),
    val modelName: String = "gemini-2.5-flash"
) {
    fun toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            role = if (role == "USER") ChatMessage.Role.USER else ChatMessage.Role.ASSISTANT,
            text = text,
            timestamp = timestamp,
            isGrounded = isGrounded,
            searchQueries = searchQueries,
            sources = sources,
            modelName = modelName
        )
    }

    companion object {
        fun fromChatMessage(msg: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = msg.id,
                role = msg.role.name,
                text = msg.text,
                timestamp = msg.timestamp,
                isGrounded = msg.isGrounded,
                searchQueries = msg.searchQueries,
                sources = msg.sources,
                modelName = msg.modelName
            )
        }
    }
}

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }

    @TypeConverter
    fun fromSourceList(value: List<GroundingSource>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        value.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("url", it.url)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toSourceList(value: String?): List<GroundingSource> {
        if (value.isNullOrBlank()) return emptyList()
        val list = mutableListOf<GroundingSource>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GroundingSource(
                        title = obj.optString("title", ""),
                        url = obj.optString("url", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
