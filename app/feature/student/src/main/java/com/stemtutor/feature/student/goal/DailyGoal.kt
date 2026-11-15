package com.stemtutor.feature.student.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.learning.LearningRepository
import com.stemtutor.core.data.model.StudentStats
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChoiceCard
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.student.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyGoalUiState(val goal: Int? = null, val error: AppError? = null)

@HiltViewModel
class DailyGoalViewModel @Inject constructor(private val learning: LearningRepository) : ViewModel() {
    private val _state = MutableStateFlow(DailyGoalUiState())
    val state: StateFlow<DailyGoalUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            learning.stats().onSuccess { s -> _state.update { it.copy(goal = s.dailyGoal) } }
        }
    }

    fun choose(goal: Int) {
        val before = _state.value.goal
        _state.update { it.copy(goal = goal, error = null) }
        viewModelScope.launch {
            learning.setDailyGoal(goal).onFailure { e -> _state.update { it.copy(goal = before, error = e.appError) } }
        }
    }
}

/** Profile → daily goal: 30, 50 or 100 XP (PRD M3). Shown for students. */
@Composable
fun DailyGoalSetting(viewModel: DailyGoalViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    DailyGoalPicker(state, viewModel::choose)
}

@Composable
fun DailyGoalPicker(state: DailyGoalUiState, onChoose: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BilingualText(R.string.ln_goal_setting, size = BilingualTextSize.Large)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StudentStats.DAILY_GOALS.forEach { goal ->
                ChoiceCard(state.goal == goal, { onChoose(goal) }, Modifier.weight(1f)) {
                    BilingualText(bilingualStringResource(R.string.ln_goal_n, goal), size = BilingualTextSize.Small)
                }
            }
        }
        state.error?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
    }
}

@BilingualPreviews
@Composable
private fun DailyGoalPreview() {
    StemTutorTheme { DailyGoalPicker(DailyGoalUiState(goal = 50)) {} }
}
