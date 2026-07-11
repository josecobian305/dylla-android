package app.dylla.models

import java.util.UUID

data class FundingStage(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var emoji: String = "",
    var order: Int = 0,
    var shortLabel: String = "",
    var automationEnabled: Boolean = false,
    var autoSMS: String = "",
    var autoEmail: String = "",
    var autoDelay: Int = 0,
    var color: String = "#007AFF"
) {
    companion object {
        val lead = FundingStage(
            id = "stage_lead",
            name = "Lead",
            emoji = "📥",
            order = 0,
            shortLabel = "Lead",
            color = "#007AFF"
        )

        val contacted = FundingStage(
            id = "stage_contacted",
            name = "Contacted",
            emoji = "📞",
            order = 1,
            shortLabel = "Contacted",
            color = "#5856D6"
        )

        val qualified = FundingStage(
            id = "stage_qualified",
            name = "Qualified",
            emoji = "✅",
            order = 2,
            shortLabel = "Qualified",
            color = "#34C759"
        )

        val submitted = FundingStage(
            id = "stage_submitted",
            name = "Submitted",
            emoji = "📤",
            order = 3,
            shortLabel = "Submitted",
            color = "#FF9500"
        )

        val approved = FundingStage(
            id = "stage_approved",
            name = "Approved",
            emoji = "🎉",
            order = 4,
            shortLabel = "Approved",
            color = "#30D158"
        )

        val funded = FundingStage(
            id = "stage_funded",
            name = "Funded",
            emoji = "💰",
            order = 5,
            shortLabel = "Funded",
            color = "#00C7BE"
        )

        val declined = FundingStage(
            id = "stage_declined",
            name = "Declined",
            emoji = "❌",
            order = 6,
            shortLabel = "Declined",
            color = "#FF3B30"
        )

        val dnc = FundingStage(
            id = "stage_dnc",
            name = "DNC",
            emoji = "🚫",
            order = 7,
            shortLabel = "DNC",
            color = "#8E8E93"
        )

        val defaults: List<FundingStage> = listOf(
            lead, contacted, qualified, submitted, approved, funded, declined, dnc
        )
    }
}
