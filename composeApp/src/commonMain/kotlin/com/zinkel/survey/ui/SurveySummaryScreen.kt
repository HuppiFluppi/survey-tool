package com.zinkel.survey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinkel.survey.config.SurveyType
import com.zinkel.survey.ui.elements.HighScore
import org.jetbrains.compose.resources.stringResource
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.survey_pages_questions
import surveytool.composeapp.generated.resources.take_quiz
import surveytool.composeapp.generated.resources.take_survey

/**
 * Composable function that displays a summary screen for a survey or quiz.
 *
 * @param surveyModel The data model containing the survey's UI state and associated actions.
 */
@Composable
fun SurveySummaryScreen(surveyModel: SurveyModel) {
    val surveySummaryUiState = (surveyModel.surveyUiState as SurveyModel.SurveyUiState.Summary).summaryUiState

    Card(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Text(text = surveySummaryUiState.title, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            Text(text = surveySummaryUiState.description, maxLines = 10, textAlign = TextAlign.Center, modifier = Modifier.padding(64.dp, 16.dp))

            if (surveySummaryUiState.highscoreEnabled) {
                HighScore(surveyModel.highscoreUiState)
            }

            Text(
                text = stringResource(Res.string.survey_pages_questions, surveySummaryUiState.totalPages, surveySummaryUiState.totalQuestions),
                modifier = Modifier.padding(16.dp)
            )

            Button(onClick = surveyModel::takeSurvey, modifier = Modifier.padding(8.dp)) {
                val buttonTxt = if (surveySummaryUiState.type == SurveyType.SURVEY)
                    stringResource(Res.string.take_survey)
                else
                    stringResource(Res.string.take_quiz)
                Text(text = buttonTxt)
            }
        }
    }
}
