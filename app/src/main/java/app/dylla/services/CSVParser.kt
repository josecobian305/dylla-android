package app.dylla.services

import app.dylla.models.CallOutcome
import app.dylla.models.Contact
import java.util.UUID

/**
 * Parses CSV (or TSV) text into a list of Contact objects.
 * Handles quoted fields, different delimiters, and flexible column name matching.
 */
object CSVParser {

    /**
     * Parse CSV text into contacts.
     * @param csvText  The raw CSV/TSV string.
     * @param listName Optional override for the list name. If blank, inferred from
     *                 filename-like patterns or defaults to "Imported List".
     * @return A Pair of (inferred list name, parsed contacts).
     */
    fun parse(csvText: String, listName: String = ""): Pair<String, List<Contact>> {
        val lines = csvText
            .split(Regex("\\r?\\n"))
            .filter { it.trim().isNotEmpty() }

        if (lines.isEmpty()) {
            return Pair(listName.ifBlank { "Imported List" }, emptyList())
        }

        // Detect delimiter: if the header line contains tabs, use tab; otherwise comma
        val headerLine = lines.first()
        val delimiter = detectDelimiter(headerLine)

        val headers = splitLine(headerLine, delimiter).map { it.lowercase().trim() }
        val dataLines = lines.drop(1)

        // Map columns by flexible name matching
        val nameIdx = findColumn(headers, listOf("name", "full name", "contact", "contact_name", "contact name"))
        val firstIdx = findColumn(headers, listOf("first", "first name", "first_name", "firstname"))
        val lastIdx = findColumn(headers, listOf("last", "last name", "last_name", "lastname", "surname"))
        val phoneIdx = findColumn(headers, listOf("phone", "mobile", "cell", "number", "phone_number", "phone number"))
        val bizIdx = findColumn(headers, listOf(
            "business", "company", "business_name", "business name",
            "company_name", "company name", "dba"
        ))
        val notesIdx = findColumn(headers, listOf("notes", "note", "comments", "comment", "memo"))
        val amtIdx = findColumn(headers, listOf(
            "amount", "requested", "amount requested", "amount_requested",
            "loan amount", "loan_amount", "funding amount", "funding_amount",
            "capital", "request amount", "request_amount"
        ))
        val dateIdx = findColumn(headers, listOf(
            "date", "date requested", "date_requested", "request date",
            "request_date", "created", "submitted", "inquiry date", "inquiry_date"
        ))
        val stateIdx = findColumn(headers, listOf("state", "st", "province"))
        val emailIdx = findColumn(headers, listOf("email", "e-mail", "email address", "email_address"))

        val knownIndices = setOfNotNull(
            nameIdx, firstIdx, lastIdx, phoneIdx, bizIdx,
            notesIdx, amtIdx, dateIdx, stateIdx, emailIdx
        )

        val contacts = mutableListOf<Contact>()

        for (line in dataLines) {
            val cols = splitLine(line, delimiter)
            if (cols.isEmpty()) continue

            fun value(idx: Int?): String {
                if (idx == null || idx >= cols.size) return ""
                return cols[idx].trim()
            }

            // Build contact name
            val contactName = when {
                nameIdx != null -> value(nameIdx)
                firstIdx != null || lastIdx != null ->
                    listOfNotNull(
                        value(firstIdx).takeIf { it.isNotBlank() },
                        value(lastIdx).takeIf { it.isNotBlank() }
                    ).joinToString(" ")
                else -> value(0)
            }

            // Find phone -- explicit column or auto-detect
            val phone = if (phoneIdx != null) {
                value(phoneIdx)
            } else {
                cols.firstOrNull { looksLikePhone(it) } ?: ""
            }

            // Skip rows with no phone
            if (phone.isBlank()) continue

            // Aggregate extra fields into notes
            val noteParts = mutableListOf<String>()
            value(amtIdx).takeIf { it.isNotBlank() }?.let { noteParts.add("Requested: $it") }
            value(dateIdx).takeIf { it.isNotBlank() }?.let { noteParts.add("Date: $it") }
            value(stateIdx).takeIf { it.isNotBlank() }?.let { noteParts.add("State: $it") }
            value(emailIdx).takeIf { it.isNotBlank() }?.let { noteParts.add("Email: $it") }
            value(notesIdx).takeIf { it.isNotBlank() }?.let { noteParts.add(it) }

            // Add any unmapped columns as "header: value"
            for (i in headers.indices) {
                if (i !in knownIndices && i < cols.size) {
                    val v = cols[i].trim()
                    if (v.isNotBlank()) {
                        noteParts.add("${headers[i]}: $v")
                    }
                }
            }

            contacts.add(
                Contact(
                    id = UUID.randomUUID().toString(),
                    name = contactName,
                    phone = phone,
                    businessName = value(bizIdx),
                    outcome = CallOutcome.PENDING,
                    notes = noteParts.joinToString(" | ")
                )
            )
        }

        val inferredName = listName.ifBlank {
            inferListName(headers, contacts)
        }

        return Pair(inferredName, contacts)
    }

    // ── Private helpers ────────────────────────────────────────────

    /**
     * Split a line respecting quoted fields.
     * Handles both comma and tab delimiters.
     */
    private fun splitLine(line: String, delimiter: Char = ','): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        fields.add(current.toString())
        return fields
    }

    /**
     * Detect whether the header uses tabs or commas.
     */
    private fun detectDelimiter(headerLine: String): Char {
        val tabCount = headerLine.count { it == '\t' }
        val commaCount = headerLine.count { it == ',' }
        return if (tabCount > commaCount) '\t' else ','
    }

    /**
     * Find a column index by trying exact matches first, then substring matches.
     */
    private fun findColumn(headers: List<String>, candidates: List<String>): Int? {
        // Exact match first
        for (candidate in candidates) {
            val idx = headers.indexOf(candidate)
            if (idx != -1) return idx
        }
        // Substring match fallback
        for (candidate in candidates) {
            val idx = headers.indexOfFirst { it.contains(candidate) }
            if (idx != -1) return idx
        }
        return null
    }

    /**
     * Check if a string looks like a phone number (7-15 digits after stripping non-digits).
     */
    private fun looksLikePhone(s: String): Boolean {
        val digits = s.replace(Regex("\\D"), "")
        return digits.length in 7..15
    }

    /**
     * Try to infer a meaningful list name from the data.
     */
    private fun inferListName(headers: List<String>, contacts: List<Contact>): String {
        // If there's a dominant business name, use it
        val bizNames = contacts.mapNotNull { it.businessName.takeIf { b -> b.isNotBlank() } }
        if (bizNames.isNotEmpty()) {
            val mostCommon = bizNames.groupingBy { it }.eachCount().maxByOrNull { it.value }
            if (mostCommon != null && mostCommon.value > contacts.size / 2) {
                return mostCommon.key
            }
        }

        return "Imported List (${contacts.size} contacts)"
    }
}
