package com.lu4p.fokuslauncher.reminders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ReminderViewModel @Inject constructor(
        private val repository: ReminderRepository,
        private val manager: ReminderManager,
        @param:ApplicationContext private val context: Context,
) : ViewModel() {
    val reminders: StateFlow<List<Reminder>> =
            repository.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<ReminderSettings> =
            repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings())

    private val _calendar = MutableStateFlow<List<ReminderMath.AgendaItem>>(emptyList())
    /** Transient: never persisted. */
    val calendar: StateFlow<List<ReminderMath.AgendaItem>> = _calendar

    fun newReminder(title: String, dueMs: Long, notify: Boolean = true) =
            Reminder(id = UUID.randomUUID().toString(), title = title.trim(), dueEpochMs = dueMs, notify = notify)

    fun save(r: Reminder) = viewModelScope.launch { manager.save(r) }
    fun delete(id: String) = viewModelScope.launch { manager.delete(id) }
    fun toggleDone(id: String) = viewModelScope.launch { manager.toggleDone(id) }

    fun setShowFullTextOnLock(value: Boolean) = viewModelScope.launch { repository.setShowFullTextOnLock(value) }

    fun setCalendarEnabled(value: Boolean) {
        viewModelScope.launch {
            repository.setCalendarEnabled(value)
            if (value) refreshCalendar(true) else _calendar.value = emptyList()
        }
    }

    /** Re-reads upcoming events when the feature is on and permitted; clears them otherwise. */
    fun refreshCalendar(enabled: Boolean = settings.value.calendarEnabled) {
        viewModelScope.launch {
            _calendar.value =
                    if (enabled && CalendarReader.hasPermission(context)) {
                        withContext(Dispatchers.IO) { CalendarReader.upcoming(context, System.currentTimeMillis()) }
                    } else {
                        emptyList()
                    }
        }
    }
}
