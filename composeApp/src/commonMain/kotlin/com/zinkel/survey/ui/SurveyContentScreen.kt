package com.zinkel.survey.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zinkel.survey.config.*
import com.zinkel.survey.ui.elements.ChoiceElement
import com.zinkel.survey.ui.elements.LikertElement
import com.zinkel.survey.ui.elements.NameElement
import com.zinkel.survey.ui.elements.PageHeader
import com.zinkel.survey.ui.elements.RatingElement
import com.zinkel.survey.ui.elements.TextElement
import org.jetbrains.compose.resources.stringResource
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.advance
import surveytool.composeapp.generated.resources.cancel
import surveytool.composeapp.generated.resources.finish

@Composable
@Preview
fun SurveyContentScreenPreview() {
    val model = SurveyModel(
        SurveyConfig(
            title = "Sample Survey", type = SurveyType.QUIZ, description = "This is a sample survey", pages = listOf(
                SurveyPage(
                    "page1", "this is page 1 description text describing what page one is about with words. It is very, very good.", content = listOf(
                        TextQuestion("text question", "1-5", score = 5), ChoiceQuestion(
                            "Choice question", "1-6", choices = listOf(ChoiceItem("first choice"), ChoiceItem("second choice"), ChoiceItem("third choice"))
                        ), NameQuestion("whats your name?", "1-7"), RatingQuestion("how well do you rate this survey?", "1-8"), LikertQuestion(
                            "how well do you rate this survey?", "1-1ß", choices = listOf("very bad", "not so bad", "quite good", "excellent"), statements = listOf(
                                LikertStatement("schwubzidität", score = 1),
                                LikertStatement("moppeligkeit", score = 1),
                                LikertStatement("schnuffigkeit", score = 1),
                            )
                        )
                    )
                )
            ), score = ScoreSettings()
        )
    )
    model.takeSurvey()
    SurveyContentScreen(model)
}

@Composable
fun SurveyContentScreen(surveyModel: SurveyModel) {
    val surveyContentUiState = (surveyModel.surveyUiState as SurveyModel.SurveyUiState.Content).contentUiState

    Card(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.background).fillMaxSize().padding(8.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val headerText = if (surveyContentUiState.totalPages == 1) {
                surveyContentUiState.surveyTitle
            } else {
                "${surveyContentUiState.surveyTitle} ${surveyContentUiState.currentPage}/${surveyContentUiState.totalPages}"
            }
            Text(
                text = headerText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp)
            )

            Divider()

            Column(modifier = Modifier.weight(1f).padding(8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PageHeader(surveyContentUiState.pageTitle, surveyContentUiState.pageDescription)

                surveyContentUiState.content.forEach {
                    when (it.type) {
                        SurveyContentType.TEXT   -> TextElement(it as TextQuestion, surveyContentUiState.showQuestionScores)
                        SurveyContentType.CHOICE -> ChoiceElement(it as ChoiceQuestion, surveyContentUiState.showQuestionScores)
                        SurveyContentType.NAME   -> NameElement(it as NameQuestion)
                        SurveyContentType.RATING -> RatingElement(it as RatingQuestion, surveyContentUiState.showQuestionScores)
                        SurveyContentType.LIKERT -> LikertElement(it as LikertQuestion, surveyContentUiState.showQuestionScores)
                    }
                }
            }

            Divider()

            Row(horizontalArrangement = SpaceBetween, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                Button(onClick = surveyModel::cancelSurvey) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(onClick = surveyModel::advanceSurvey) {
                    if (surveyContentUiState.currentPage == surveyContentUiState.totalPages) Text(stringResource(Res.string.finish))
                    else Text(stringResource(Res.string.advance))
                }
            }
        }
    }
}
