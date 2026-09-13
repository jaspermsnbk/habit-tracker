package com.example.habit_tracker_2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.habit_tracker_2.HabitApplication
import com.example.habit_tracker_2.data.HabitRepository
import com.example.habit_tracker_2.data.HabitUi
import com.example.habit_tracker_2.data.LabelUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Exposes habit state to the UI as a [StateFlow] and forwards user actions to the
 * repository. The UI is a pure function of [habits] and [labels]; it never touches the data layer directly.
 */
class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

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

    fun deleteHabit(habitId: String) = viewModelScope.launch {
        repository.deleteHabit(habitId)
    }

    companion object {
        /** Builds the ViewModel with the repository pulled from the Application. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HabitApplication
                HabitViewModel(app.repository)
            }
        }
    }
}
