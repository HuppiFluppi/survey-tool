package com.zinkel.survey.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

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
