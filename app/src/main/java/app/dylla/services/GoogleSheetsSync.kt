package app.dylla.services

import app.dylla.models.CallList
import app.dylla.models.Contact
import app.dylla.models.FundingStage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

object GoogleSheetsSync {

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun pushContact(contact: Contact, stage: FundingStage?, listName: String) {
        val profile = PersistenceManager.loadUserProfile()
        val webhookURL = profile.googleSheetsWebhookURL
        if (webhookURL.isBlank()) return

        val payload = contactToMap(contact, stage, listName)

        CoroutineScope(Dispatchers.IO).launch {
            post(webhookURL, gson.toJson(payload))
        }
    }

    fun pushAllContacts(list: CallList, stages: List<FundingStage>) {
        val profile = PersistenceManager.loadUserProfile()
        val webhookURL = profile.googleSheetsWebhookURL
        if (webhookURL.isBlank()) return

        val rows = list.contacts.map { contact ->
            val stage = stages.firstOrNull { it.id == contact.stageID }
            contactToMap(contact, stage, list.name)
        }

        val payload = mapOf("batch" to true, "rows" to rows)

        CoroutineScope(Dispatchers.IO).launch {
            post(webhookURL, gson.toJson(payload))
        }
    }

    private fun contactToMap(contact: Contact, stage: FundingStage?, listName: String): Map<String, String> {
        return mapOf(
            "name" to contact.name,
            "phone" to contact.phone,
            "phones" to contact.phone,
            "business" to contact.businessName,
            "outcome" to contact.outcome.label,
            "stage" to (stage?.name ?: ""),
            "notes" to contact.notes,
            "callTime" to (contact.callTime?.let { dateFormat.format(it) } ?: ""),
            "callbackDate" to (contact.callbackDate?.let { dateFormat.format(it) } ?: ""),
            "fundedAmount" to contact.fundedAmount,
            "paymentTerm" to contact.paymentTerm,
            "list" to listName,
            "email" to extractEmail(contact.notes)
        )
    }

    fun extractEmail(notes: String): String {
        if (notes.isBlank()) return ""
        val parts = notes.split("|").map { it.trim() }
        for (part in parts) {
            if (part.startsWith("Email:", ignoreCase = true)) {
                return part.substringAfter(":").trim()
            }
        }
        return ""
    }

    private fun post(urlString: String, body: String) {
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }
}
