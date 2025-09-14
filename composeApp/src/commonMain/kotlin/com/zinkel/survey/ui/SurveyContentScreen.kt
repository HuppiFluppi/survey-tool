package com.zinkel.survey.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zinkel.survey.config.*
import com.zinkel.survey.data.ChoiceSurveyContentData
import com.zinkel.survey.data.LikertSurveyContentData
import com.zinkel.survey.data.NameSurveyContentData
import com.zinkel.survey.data.RatingSurveyContentData
import com.zinkel.survey.data.TextSurveyContentData
import com.zinkel.survey.ui.elements.ChoiceElement
import com.zinkel.survey.ui.elements.LikertElement
import com.zinkel.survey.ui.elements.NameElement
import com.zinkel.survey.ui.elements.PageHeader
import com.zinkel.survey.ui.elements.RatingElement
import com.zinkel.survey.ui.elements.TextElement
import org.jetbrains.compose.resources.stringResource
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.advance
import surveytool.composeapp.generated.resources.back
import surveytool.composeapp.generated.resources.cancel
import surveytool.composeapp.generated.resources.finish
import java.io.File

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
                            "how well do you rate this survey?",
                            "1-1ß",
                            choices = listOf("very bad", "not so bad", "quite good", "excellent"),
                            statements = listOf(
                                LikertStatement("schwubzidität", score = 1),
                                LikertStatement("moppeligkeit", score = 1),
                                LikertStatement("schnuffigkeit", score = 1),
                            )
                        )
                    )
                )
            ), score = ScoreSettings()
        ),
        File("test.yaml"),
        rememberCoroutineScope()
    )
    model.takeSurvey()
    SurveyContentScreen(model)
}

/**
 * A composable function that displays the content of a running survey. It manages the UI
 * interactions and state for various types of survey questions such as text, choice, rating,
 * and others. The function also manages navigation between different pages within the survey.
 *
 * @param surveyModel The [SurveyModel] instance containing the state and logic for the survey,
 * including survey data, user inputs, and navigation actions.
 */
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
            if (surveyContentUiState.totalPages > 1) {
                val animatedProgress by animateFloatAsState(targetValue = (surveyContentUiState.currentPage - 1).toFloat() / surveyContentUiState.totalPages.toFloat())
                LinearProgressIndicator(
                    progress = animatedProgress,
                    backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val scrollState = rememberScrollState()
            LaunchedEffect(surveyContentUiState.currentPage) {
                scrollState.animateScrollTo(0)
            }
            Column(modifier = Modifier.weight(1f).padding(8.dp).verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PageHeader(surveyContentUiState.pageTitle, surveyContentUiState.pageDescription)

                surveyContentUiState.content.forEach {
                    when (it) {
                        is TextSurveyContentData   -> TextElement(
                            it.question,
                            surveyContentUiState.showQuestionScores,
                            { answer -> surveyModel.updateAnswer(it.question.id, answer) },
                            it.answer ?: "",
                            surveyContentUiState.inputErrors[it.question.id]
                        )

                        is ChoiceSurveyContentData -> ChoiceElement(
                            it.question,
                            surveyContentUiState.showQuestionScores,
                            { answer -> surveyModel.updateAnswer(it.question.id, answer) },
                            it.answer ?: emptyList(),
                            surveyContentUiState.inputErrors[it.question.id]
                        )

                        is NameSurveyContentData   -> NameElement(
                            it.question,
                            { answer -> surveyModel.updateAnswer(it.question.id, answer) },
                            it.answer ?: "",
                            surveyContentUiState.inputErrors[it.question.id]
                        )

                        is RatingSurveyContentData -> RatingElement(
                            it.question,
                            { answer -> surveyModel.updateAnswer(it.question.id, answer) },
                            it.answer ?: 0,
                            surveyContentUiState.inputErrors[it.question.id]
                        )

                        is LikertSurveyContentData -> LikertElement(
                            it.question,
                            surveyContentUiState.showQuestionScores,
                            { statement, choice -> surveyModel.updateAnswer(it.question.id, statement, choice) },
                            it.answer ?: emptyMap(),
                            surveyContentUiState.inputErrors[it.question.id]
                        )
                    }
                }
            }

            Divider(thickness = 2.dp)

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = surveyModel::cancelSurvey, modifier = Modifier.padding(8.dp, 4.dp)) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (surveyContentUiState.currentPage > 1) {
                    Button(onClick = surveyModel::backSurvey, modifier = Modifier.padding(4.dp, 4.dp)) {
                        Text(stringResource(Res.string.back))
                    }
                }
                Button(onClick = surveyModel::advanceSurvey, modifier = Modifier.padding(4.dp, 4.dp)) {
                    if (surveyContentUiState.currentPage == surveyContentUiState.totalPages) Text(stringResource(Res.string.finish))
                    else Text(stringResource(Res.string.advance))
                }
            }
        }
    }
}
