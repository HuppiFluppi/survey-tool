package com.zinkel.survey.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Displays the survey application UI using the provided survey loading state.
 * Main entry point for the survey application after it has been loaded.
 * Showing either a summary screen or the survey content while the survey is running.
 *
 * @param surveyLoadUiState A state containing the loaded survey configuration and related file.
 *                          This parameter must always be of type `SurveyLoadUiState.Loaded`.
 */
@Composable
fun SurveyApp(surveyLoadUiState: SurveyLoadUiState.Loaded) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme) {
        val coroutineScope = rememberCoroutineScope()
        val surveyModel = remember { SurveyModel(surveyLoadUiState.config, surveyLoadUiState.configFile, coroutineScope) }

        when (surveyModel.surveyUiState) {
            is SurveyModel.SurveyUiState.Content -> SurveyContentScreen(surveyModel)
            is SurveyModel.SurveyUiState.Summary -> SurveySummaryScreen(surveyModel)
        }
    }
}
