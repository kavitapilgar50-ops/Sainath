package com.example.data.repository

import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiResponse
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MyraRepository(private val chatDao: ChatDao) {

    fun getMessages(): Flow<List<ChatMessage>> {
        return chatDao.getAllMessages().map { entities ->
            entities.map { it.toChatMessage() }
        }
    }

    suspend fun saveUserMessage(text: String): ChatMessage {
        val userMsg = ChatMessage(
            role = ChatMessage.Role.USER,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        val id = chatDao.insertMessage(ChatMessageEntity.fromChatMessage(userMsg))
        return userMsg.copy(id = id)
    }

    suspend fun saveAssistantMessage(response: GeminiResponse, modelName: String): ChatMessage {
        val assistantMsg = ChatMessage(
            role = ChatMessage.Role.ASSISTANT,
            text = response.text,
            timestamp = System.currentTimeMillis(),
            isGrounded = response.isGrounded,
            searchQueries = response.searchQueries,
            sources = response.sources,
            modelName = modelName
        )
        val id = chatDao.insertMessage(ChatMessageEntity.fromChatMessage(assistantMsg))
        return assistantMsg.copy(id = id)
    }

    suspend fun queryGemini(
        prompt: String,
        apiKeyOverride: String?,
        modelName: String,
        enableSearchGrounding: Boolean,
        recentHistory: List<Pair<String, String>>
    ): Result<GeminiResponse> {
        return GeminiApiClient.generateContent(
            prompt = prompt,
            apiKeyOverride = apiKeyOverride,
            modelName = modelName,
            enableSearchGrounding = enableSearchGrounding,
            conversationHistory = recentHistory
        )
    }

    suspend fun clearChat() {
        chatDao.clearAllMessages()
    }

    suspend fun deleteMessage(id: Long) {
        chatDao.deleteMessageById(id)
    }
}
