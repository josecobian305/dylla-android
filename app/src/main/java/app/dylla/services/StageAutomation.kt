package app.dylla.services

import android.util.Log
import app.dylla.models.Contact
import app.dylla.models.FundingStage

/**
 * Simple stage-change automation service.
 * When a contact's stage changes, checks if the target stage has automation enabled.
 * If so, logs the action and marks the contact's automationSent flag.
 *
 * This is a placeholder implementation -- actual SMS/email dispatch would be
 * wired to TextTorrent / SendGrid in production.
 */
object StageAutomation {

    private const val TAG = "StageAutomation"

    /**
     * Called when a contact's stage changes.
     * @param contact   The contact whose stage changed.
     * @param newStage  The stage the contact is moving to.
     * @param stages    Full list of stages (for lookup).
     * @return The contact with automationSent updated if automation fired.
     */
    fun onStageChanged(
        contact: Contact,
        newStage: FundingStage,
        stages: List<FundingStage>
    ): Contact {
        if (!newStage.automationEnabled) {
            Log.d(TAG, "Stage '${newStage.name}' has automation disabled, skipping for ${contact.name}")
            return contact
        }

        // Log what would be sent
        if (newStage.autoSMS.isNotBlank()) {
            val smsBody = interpolateTemplate(newStage.autoSMS, contact)
            Log.i(TAG, "AUTOMATION [SMS] -> ${contact.phone}: $smsBody")
        }

        if (newStage.autoEmail.isNotBlank()) {
            val emailBody = interpolateTemplate(newStage.autoEmail, contact)
            Log.i(TAG, "AUTOMATION [Email] -> ${contact.name}: $emailBody")
        }

        // Mark automation as sent
        contact.automationSent = true
        Log.i(TAG, "Automation sent flag set for contact '${contact.name}' at stage '${newStage.name}'")

        return contact
    }

    /**
     * Check and fire automation for a batch of contacts at a given stage.
     * Skips contacts that already have automationSent = true.
     * @return Updated list of contacts.
     */
    fun processContacts(
        contacts: List<Contact>,
        stage: FundingStage,
        stages: List<FundingStage>
    ): List<Contact> {
        return contacts.map { contact ->
            if (contact.stageID == stage.id && !contact.automationSent) {
                onStageChanged(contact, stage, stages)
            } else {
                contact
            }
        }
    }

    /**
     * Simple template interpolation: replaces {name}, {business}, {phone}
     * with contact fields.
     */
    private fun interpolateTemplate(template: String, contact: Contact): String {
        return template
            .replace("{name}", contact.name)
            .replace("{business}", contact.businessName)
            .replace("{phone}", contact.phone)
    }
}
