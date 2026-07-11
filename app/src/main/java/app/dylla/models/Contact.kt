package app.dylla.models

import java.util.UUID
import java.util.Date

enum class CallOutcome(val label: String, val emoji: String, val color: String) {
    PENDING("Pending", "🔵", "blue"),
    ANSWERED("Answered", "✅", "green"),
    NO_ANSWER("No Answer", "📵", "orange"),
    CALLBACK("Callback", "📅", "purple"),
    DNC("DNC", "🚫", "red"),
    SKIPPED("Skipped", "⏭", "gray");

    val isDone: Boolean get() = this != PENDING
}

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var phone: String = "",
    var businessName: String = "",
    var outcome: CallOutcome = CallOutcome.PENDING,
    var callTime: Date? = null,
    var notes: String = "",
    var callNotes: String = "",
    var stageID: String? = null,
    var callbackDate: Date? = null,
    var automationSent: Boolean = false,
    var nudgeSent: Boolean = false,
    var fundedAmount: String = "",
    var paymentTerm: String = "",
    var monthFunded: String = ""
) {
    fun stage(stages: List<FundingStage>): FundingStage? {
        val sid = stageID ?: return null
        return stages.firstOrNull { it.id == sid }
    }
}
