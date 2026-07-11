package app.dylla.models

import java.util.UUID

data class TwilioConfig(
    var sid: String = "",
    var token: String = "",
    var number: String = "",
    var numbers: List<String> = emptyList()
) {
    val isConfigured: Boolean
        get() = sid.isNotBlank() && token.isNotBlank() && number.isNotBlank()

    val allNumbers: List<String>
        get() {
            val set = mutableListOf<String>()
            if (number.isNotBlank()) set.add(number)
            numbers.forEach { if (it.isNotBlank() && it !in set) set.add(it) }
            return set
        }
}

enum class IndustryType(val label: String, val emoji: String) {
    RESTAURANT("Restaurant", "🍽️"),
    RETAIL("Retail", "🛒"),
    MEDICAL("Medical", "🏥"),
    LEGAL("Legal", "⚖️"),
    CONSTRUCTION("Construction", "🚧"),
    REAL_ESTATE("Real Estate", "🏠"),
    AUTOMOTIVE("Automotive", "🚗"),
    SALON("Salon", "💇"),
    FITNESS("Fitness", "🏋️"),
    TECH("Tech", "💻"),
    OTHER("Other", "🏢")
}

data class Company(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var industry: IndustryType = IndustryType.OTHER,
    var spoofNumbers: List<String> = emptyList(),
    var isDefault: Boolean = false
)

data class UserProfile(
    var twilio: TwilioConfig = TwilioConfig(),
    var userPhone: String = "",
    var linesPerBatch: Int = 10,
    var googleSheetsWebhookURL: String = "",
    var emailSignature: String = "",
    var apiKey: String = "",
    var companies: List<Company> = emptyList()
) {
    val defaultCompany: Company?
        get() = companies.firstOrNull { it.isDefault } ?: companies.firstOrNull()
}
