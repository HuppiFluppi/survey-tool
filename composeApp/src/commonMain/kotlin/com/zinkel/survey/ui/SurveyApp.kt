package com.zinkel.survey.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun SurveyApp(surveyLoadUiState: SurveyLoadUiState.Loaded) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme) {
        val surveyModel = remember { SurveyModel(surveyLoadUiState.config) }

        when (surveyModel.surveyUiState) {
            is SurveyModel.SurveyUiState.Content -> SurveyContentScreen(surveyModel)
            is SurveyModel.SurveyUiState.Summary -> SurveySummaryScreen(surveyModel)
        }
    }
}
