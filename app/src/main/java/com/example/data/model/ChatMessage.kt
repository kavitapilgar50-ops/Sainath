package com.example.data.model

data class GroundingSource(
    val title: String,
    val url: String
)

data class ChatMessage(
    val id: Long = 0,
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGrounded: Boolean = false,
    val searchQueries: List<String> = emptyList(),
    val sources: List<GroundingSource> = emptyList(),
    val modelName: String = "gemini-2.5-flash"
) {
    enum class Role {
        USER,
        ASSISTANT
    }
}

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}
