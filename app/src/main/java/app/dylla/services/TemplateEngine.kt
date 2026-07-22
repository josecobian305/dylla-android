package app.dylla.services

import app.dylla.models.Contact
import app.dylla.models.UserProfile

object TemplateEngine {

    fun resolve(template: String, contact: Contact, profile: UserProfile): String {
        return template
            .replace("{name}", contact.name)
            .replace("{business}", contact.businessName)
            .replace("{company}", contact.businessName)
            .replace("{agent}", profile.defaultCompany?.name ?: "")
            .replace("{applyURL}", profile.googleSheetsWebhookURL)
    }

    fun resolve(template: String, name: String = "", profile: UserProfile): String {
        return template
            .replace("{name}", name)
            .replace("{business}", "")
            .replace("{company}", "")
            .replace("{agent}", profile.defaultCompany?.name ?: "")
            .replace("{applyURL}", profile.googleSheetsWebhookURL)
    }
}
