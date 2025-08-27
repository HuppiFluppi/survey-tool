package com.zinkel.survey.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SurveyLoadApp(surveyLoadUiState: SurveyLoadUiState) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme) {
        val coroutineScope = rememberCoroutineScope()

        SurveyLoadScreen(surveyLoadUiState) {
            coroutineScope.launch { SurveyLoadModel.load(it) }
        }
    }
}
