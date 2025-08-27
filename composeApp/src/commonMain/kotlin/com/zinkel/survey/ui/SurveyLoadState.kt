package com.zinkel.survey.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zinkel.survey.config.SurveyConfig
import com.zinkel.survey.config.SurveyConfigLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SurveyLoadModel {
    var surveyLoadUiState by mutableStateOf<SurveyLoadUiState>(SurveyLoadUiState.NotLoaded)
        private set

    suspend fun load(file: File) {
        try {
            surveyLoadUiState = SurveyLoadUiState.Loading
            val config = withContext(Dispatchers.Default) { SurveyConfigLoader.load(file) }
            surveyLoadUiState = SurveyLoadUiState.Loaded(config)
        } catch (e: Exception) {
            surveyLoadUiState = SurveyLoadUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun reset() {
        surveyLoadUiState = SurveyLoadUiState.NotLoaded
    }
}

sealed class SurveyLoadUiState {
    data object NotLoaded : SurveyLoadUiState()
    data object Loading : SurveyLoadUiState()
    data class Loaded(val config: SurveyConfig) : SurveyLoadUiState()
    data class Error(val message: String) : SurveyLoadUiState()
}
