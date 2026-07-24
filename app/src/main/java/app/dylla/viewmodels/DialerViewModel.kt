package app.dylla.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import app.dylla.models.CallList
import app.dylla.models.CallOutcome
import app.dylla.models.Company
import app.dylla.models.Contact
import app.dylla.models.FundingStage
import app.dylla.services.CSVParser
import app.dylla.services.PersistenceManager
import app.dylla.services.StageAutomation
import java.util.Date
import java.util.UUID

/**
 * Main ViewModel for the dialer / CRM screen.
 * Manages call lists, contacts, funding stages, session state, and persistence.
 */
class DialerViewModel : ViewModel() {

    // ── Observable state ───────────────────────────────────────────

    var lists by mutableStateOf<List<CallList>>(emptyList())
        private set

    var activeList by mutableStateOf<CallList?>(null)
        private set

    var currentIndex by mutableStateOf<Int?>(null)
        private set

    var isDialing by mutableStateOf(false)
        private set

    var showOutcome by mutableStateOf(false)
        private set

    var nudgesFiredCount by mutableIntStateOf(0)
        private set

    var stages by mutableStateOf<List<FundingStage>>(emptyList())
        private set

    var activeCompany by mutableStateOf<Company?>(null)
        private set

    var powerDialerEnabled by mutableStateOf(false)
        private set

    var localPresenceEnabled by mutableStateOf(false)
        private set

    // ── Computed properties ────────────────────────────────────────

    val currentContact: Contact?
        get() {
            val list = activeList ?: return null
            val idx = currentIndex ?: return null
            return list.contacts.getOrNull(idx)
        }

    val pendingCount: Int
        get() = activeList?.contacts?.count { it.outcome == CallOutcome.PENDING } ?: 0

    val doneCount: Int
        get() = activeList?.contacts?.count { it.outcome.isDone } ?: 0

    val callbackCount: Int
        get() = activeList?.contacts?.count { it.outcome == CallOutcome.CALLBACK } ?: 0

    val calledTodayCount: Int
        get() {
            val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                .format(Date())
            return activeList?.contacts?.count { contact ->
                contact.callTime?.let { callTime ->
                    java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                        .format(callTime) == todayStr
                } ?: false
            } ?: 0
        }

    val progress: Float
        get() = activeList?.progress ?: 0f

    val stageBreakdown: List<Pair<FundingStage, Int>>
        get() {
            val contacts = activeList?.contacts ?: return emptyList()
            val counts = mutableMapOf<String, Int>()
            for (contact in contacts) {
                val sid = contact.stageID ?: continue
                counts[sid] = (counts[sid] ?: 0) + 1
            }
            return stages
                .filter { counts.containsKey(it.id) }
                .map { stage -> stage to (counts[stage.id] ?: 0) }
        }

    /** All callbacks across all lists, sorted by date ascending. */
    val allCallbacks: List<Contact>
        get() = lists
            .flatMap { it.contacts }
            .filter { it.outcome == CallOutcome.CALLBACK && it.callbackDate != null }
            .sortedBy { it.callbackDate }

    /** All called contacts across all lists, sorted most recent first. */
    val allHistory: List<Contact>
        get() = lists
            .flatMap { it.contacts }
            .filter { it.callTime != null }
            .sortedByDescending { it.callTime }

    // ── Init ───────────────────────────────────────────────────────

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        stages = PersistenceManager.loadStages()
        lists = PersistenceManager.loadLists()
        val savedActiveId = PersistenceManager.loadActiveListId()
        activeList = if (savedActiveId != null) {
            lists.firstOrNull { it.id == savedActiveId }
        } else {
            lists.firstOrNull()
        }
    }

    // ── List management ────────────────────────────────────────────

    /**
     * Import a CSV string as a new call list.
     * @param text     Raw CSV/TSV content.
     * @param listName Optional name for the list.
     * @return The newly created CallList, or null if parsing produced no contacts.
     */
    fun importCSV(text: String, listName: String = ""): CallList? {
        val (inferredName, contacts) = CSVParser.parse(text, listName)
        if (contacts.isEmpty()) return null

        val newList = CallList(
            id = UUID.randomUUID().toString(),
            name = inferredName,
            contacts = contacts.toMutableList()
        )

        lists = lists + newList
        activeList = newList
        currentIndex = null
        persist()
        return newList
    }

    fun importContacts(listName: String, contacts: List<Contact>): CallList? {
        if (contacts.isEmpty()) return null

        val newList = CallList(
            id = UUID.randomUUID().toString(),
            name = listName.ifBlank { "Imported List" },
            contacts = contacts.toMutableList()
        )

        lists = lists + newList
        activeList = newList
        currentIndex = null
        persist()
        return newList
    }

    fun selectList(list: CallList) {
        activeList = list
        currentIndex = null
        isDialing = false
        showOutcome = false
        PersistenceManager.saveActiveListId(list.id)
    }

    fun deleteList(listId: String) {
        lists = lists.filter { it.id != listId }
        if (activeList?.id == listId) {
            activeList = lists.firstOrNull()
        }
        persist()
    }

    fun renameList(listId: String, newName: String) {
        lists = lists.map { list ->
            if (list.id == listId) list.copy(name = newName) else list
        }
        if (activeList?.id == listId) {
            activeList = lists.firstOrNull { it.id == listId }
        }
        persist()
    }

    // ── Session control ────────────────────────────────────────────

    /**
     * Start a dialing session. Sets currentIndex to the first pending contact.
     */
    fun startSession() {
        val list = activeList ?: return
        val firstPending = list.contacts.indexOfFirst { it.outcome == CallOutcome.PENDING }
        currentIndex = if (firstPending >= 0) firstPending else null
        isDialing = true
        showOutcome = false
    }

    /**
     * Advance to the next pending contact in the active list.
     */
    fun nextContact() {
        val list = activeList ?: return
        val startIdx = (currentIndex ?: -1) + 1

        // Search from current position forward, then wrap around
        for (offset in 0 until list.contacts.size) {
            val idx = (startIdx + offset) % list.contacts.size
            if (list.contacts[idx].outcome == CallOutcome.PENDING) {
                currentIndex = idx
                showOutcome = false
                return
            }
        }

        // No more pending contacts
        currentIndex = null
        isDialing = false
        showOutcome = false
    }

    /**
     * Record the outcome of a call for a contact.
     */
    fun setOutcome(
        contact: Contact,
        outcome: CallOutcome,
        notes: String = "",
        callbackDate: Date? = null,
        stageID: String? = null
    ) {
        val list = activeList ?: return

        val updatedContacts = list.contacts.map { c ->
            if (c.id == contact.id) {
                var updated = c.copy().apply {
                    this.outcome = outcome
                    this.callTime = Date()
                    this.callNotes = notes
                    this.callbackDate = callbackDate
                    this.stageID = stageID ?: c.stageID
                }

                // Fire stage automation if stage changed
                if (stageID != null && stageID != c.stageID) {
                    val newStage = stages.firstOrNull { it.id == stageID }
                    if (newStage != null) {
                        updated = StageAutomation.onStageChanged(updated, newStage, stages)
                    }
                }

                updated
            } else {
                c
            }
        }

        val updatedList = list.copy(contacts = updatedContacts.toMutableList())
        lists = lists.map { if (it.id == updatedList.id) updatedList else it }
        activeList = updatedList

        showOutcome = false

        // Auto-advance in power dialer mode
        if (powerDialerEnabled) {
            nextContact()
        }

        persist()
    }

    /**
     * Update a single contact's fields.
     */
    fun updateContact(listId: String, contactId: String, transform: (Contact) -> Contact) {
        lists = lists.map { list ->
            if (list.id == listId) {
                list.copy(
                    contacts = list.contacts.map { c ->
                        if (c.id == contactId) transform(c) else c
                    }.toMutableList()
                )
            } else {
                list
            }
        }
        if (activeList?.id == listId) {
            activeList = lists.firstOrNull { it.id == listId }
        }
        persist()
    }

    // ── Stage management ───────────────────────────────────────────

    fun updateStages(newStages: List<FundingStage>) {
        stages = newStages
        PersistenceManager.saveStages(stages)
    }

    fun resetStages() {
        stages = FundingStage.defaults
        PersistenceManager.saveStages(stages)
    }

    // ── Settings toggles ──────────────────────────────────────────

    fun togglePowerDialer() {
        powerDialerEnabled = !powerDialerEnabled
    }

    fun toggleLocalPresence() {
        localPresenceEnabled = !localPresenceEnabled
    }

    fun switchCompany(company: Company?) {
        activeCompany = company
    }

    // ── Clear ──────────────────────────────────────────────────────

    fun clearAll() {
        lists = emptyList()
        activeList = null
        currentIndex = null
        isDialing = false
        showOutcome = false
        nudgesFiredCount = 0
        persist()
    }

    // ── Persistence ────────────────────────────────────────────────

    private fun persist() {
        PersistenceManager.saveLists(lists)
        PersistenceManager.saveActiveListId(activeList?.id)
    }
}
