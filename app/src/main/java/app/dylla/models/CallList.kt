package app.dylla.models

import java.util.UUID

data class CallList(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var contacts: MutableList<Contact> = mutableListOf(),
    var createdAt: Long = System.currentTimeMillis()
) {
    val totalCount: Int get() = contacts.size
    val pendingCount: Int get() = contacts.count { it.outcome == CallOutcome.PENDING }
    val doneCount: Int get() = contacts.count { it.outcome.isDone }
    val callbackCount: Int get() = contacts.count { it.outcome == CallOutcome.CALLBACK }
    val progress: Float get() = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f
}
