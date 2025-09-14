package com.zinkel.survey.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Sets up the Survey Load application interface using the provided UI state.
 * The function implements the callback logic triggered when a file is selected for loading.
 *
 * @param surveyLoadUiState Describes the current state of the survey loading process. It may indicate
 * whether no action has been performed, a survey is being loaded, the loading is successful, or an error
 * has occurred during the loading process.
 */
@Composable
fun SurveyLoadApp(surveyLoadUiState: SurveyLoadUiState) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme) {
        val coroutineScope = rememberCoroutineScope()

        SurveyLoadScreen(surveyLoadUiState) {
            coroutineScope.launch { SurveyLoadModel.load(it) }
        }
    }
}
