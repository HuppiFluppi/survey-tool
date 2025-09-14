package com.zinkel.survey.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zinkel.survey.config.SurveyConfig
import com.zinkel.survey.config.SurveyConfigLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * An object responsible for managing the state and loading of survey configuration files.
 *
 * This model functions as the state holder for the survey loading process, maintaining
 * the current UI state through observable properties. It supports loading survey configuration
 * files asynchronously and updates its state accordingly to reflect the loading status,
 * success, or failure.
 */
object SurveyLoadModel {
    var surveyLoadUiState by mutableStateOf<SurveyLoadUiState>(SurveyLoadUiState.NotLoaded)
        private set

    /**
     * Loads a survey configuration from the specified file and updates the UI state accordingly.
     * The method runs the file loading operation on a background thread and handles the state
     * transitions for loading, success, or failure.
     *
     * @param file The file from which to load the survey configuration.
     */
    suspend fun load(file: File) {
        try {
            surveyLoadUiState = SurveyLoadUiState.Loading
            val config = withContext(Dispatchers.Default) { SurveyConfigLoader.load(file) }
            surveyLoadUiState = SurveyLoadUiState.Loaded(config, file)
        } catch (e: Exception) {
            e.printStackTrace()
            surveyLoadUiState = SurveyLoadUiState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Resets the survey loading state to its initial state.
     *
     * This function sets the `surveyLoadUiState` to `SurveyLoadUiState.NotLoaded`,
     * indicating that no survey data has been loaded or is currently being processed.
     * It is generally used to clear any previously loaded survey state or errors,
     * allowing the application to start fresh.
     */
    fun reset() {
        surveyLoadUiState = SurveyLoadUiState.NotLoaded
    }
}

/**
 * Represents the different states of the UI while loading a survey or quiz configuration.
 *
 * This sealed class defines the possible states the UI can be in during the survey loading process,
 * allowing the application to handle them explicitly. These states include not yet attempting to load,
 * ongoing loading, successful loading with configuration data, or an error state.
 */
sealed class SurveyLoadUiState {
    data object NotLoaded : SurveyLoadUiState()
    data object Loading : SurveyLoadUiState()
    data class Loaded(val config: SurveyConfig, val configFile: File) : SurveyLoadUiState()
    data class Error(val message: String) : SurveyLoadUiState()
}
