package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary

data class QuickPrompt(
    val title: String,
    val prompt: String,
    val forceSearch: Boolean = false
)

val DEFAULT_QUICK_PROMPTS = listOf(
    QuickPrompt("⚡ आज की बड़ी खबरें", "आज की बड़ी खबरों की ताज़ा अपडेट्स दीजिए (टॉप न्यूज़, बिजनेस और टेक)।", forceSearch = true),
    QuickPrompt("🌤️ आज का मौसम", "आज भारत के मुख्य शहरों में मौसम का क्या हाल है? संक्षेप में बताइए।", forceSearch = true),
    QuickPrompt("📈 टेक & बिजनेस", "आज के प्रमुख टेक और मार्केट अपडेट्स क्या हैं?", forceSearch = true),
    QuickPrompt("🏏 स्पोर्ट्स राउंडअप", "आज के क्रिकेट और स्पोर्ट्स मैच अपडेट्स क्या हैं?", forceSearch = true),
    QuickPrompt("💻 Python DNS Script", "Write a clean Python script to resolve DNS records and measure latency. Explain key points briefly.", forceSearch = false),
    QuickPrompt("🧠 Multi-Step Logic", "Solve step-by-step: If a server cluster doubles load every 3 hours, calculate time to reach 64x capacity.", forceSearch = false)
)

@Composable
fun QuickPromptCapsules(
    onPromptClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    prompts: List<QuickPrompt> = DEFAULT_QUICK_PROMPTS
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("quick_prompt_capsules_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        prompts.forEach { promptItem ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .clickable {
                        onPromptClick(promptItem.prompt, promptItem.forceSearch)
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = promptItem.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
