package com.jaspermsnbk.habit_tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaspermsnbk.habit_tracker.HabitApplication
import com.jaspermsnbk.habit_tracker.data.AppPreferences
import com.jaspermsnbk.habit_tracker.data.HabitRepository
import com.jaspermsnbk.habit_tracker.data.HabitUi
import com.jaspermsnbk.habit_tracker.data.LabelUi
import com.jaspermsnbk.habit_tracker.data.PreferencesStore
import com.jaspermsnbk.habit_tracker.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Exposes habit state to the UI as a [StateFlow] and forwards user actions to the
 * repository. The UI is a pure function of [habits] and [labels]; it never touches the data layer directly.
 */
class HabitViewModel(
    private val repository: HabitRepository,
    private val preferencesStore: PreferencesStore,
) : ViewModel() {

    val habits: StateFlow<List<HabitUi>> =
        repository.habits.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val labels: StateFlow<List<LabelUi>> =
        repository.labels.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Settings choices. Already current when first read, so screens open with the saved values. */
    val preferences: StateFlow<AppPreferences> = preferencesStore.preferences

    fun setTheme(theme: ThemeMode) = preferencesStore.setTheme(theme)

    fun setDynamicColor(enabled: Boolean) = preferencesStore.setDynamicColor(enabled)

    fun setFirstDayOfWeek(day: DayOfWeek?) = preferencesStore.setFirstDayOfWeek(day)

    internal fun setTrendsRange(range: TrendRange) = preferencesStore.setTrendsRange(range.name)

    fun setReminderEnabled(enabled: Boolean) = preferencesStore.setReminderEnabled(enabled)

    fun setReminderTime(time: LocalTime) = preferencesStore.setReminderTime(time)

    fun addHabit(name: String, color: String, labelId: String? = null) = viewModelScope.launch {
        repository.addHabit(name, color, labelId)
    }

    fun addLabel(name: String) = viewModelScope.launch {
        repository.addLabel(name)
    }

    fun renameLabel(labelId: String, name: String) = viewModelScope.launch {
        repository.renameLabel(labelId, name)
    }

    fun deleteLabel(labelId: String) = viewModelScope.launch {
        repository.deleteLabel(labelId)
    }

    fun toggleToday(habitId: String) = viewModelScope.launch {
        repository.toggleToday(habitId)
    }

    fun updateHabit(habitId: String, name: String, color: String, labelId: String?) = viewModelScope.launch {
        repository.updateHabit(habitId, name, color, labelId)
    }

    fun deleteHabit(habitId: String) = viewModelScope.launch {
        repository.deleteHabit(habitId)
    }

    fun deleteAllData() = viewModelScope.launch {
        repository.deleteAllData()
    }

    companion object {
        /** Builds the ViewModel with the repository pulled from the Application. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HabitApplication
                HabitViewModel(app.repository, app.preferencesStore)
            }
        }
    }
}
