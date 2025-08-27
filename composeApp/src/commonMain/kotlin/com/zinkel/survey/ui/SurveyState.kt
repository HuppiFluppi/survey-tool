package com.zinkel.survey.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zinkel.survey.config.SurveyConfig
import com.zinkel.survey.config.SurveyPageContent
import com.zinkel.survey.config.SurveyType

class SurveyModel(val surveyConfig: SurveyConfig) {
    private val surveySummaryUiState = SurveySummaryUiState(
        title = surveyConfig.title,
        type = surveyConfig.type,
        totalPages = surveyConfig.pages.size,
        totalQuestions = surveyConfig.pages.sumOf { it.content.size },
        description = surveyConfig.description,
        highscoreEnabled = surveyConfig.type == SurveyType.QUIZ && surveyConfig.score.showLeaderboard
    )

    sealed class SurveyUiState {
        data class Summary(val summaryUiState: SurveySummaryUiState) : SurveyUiState()
        data class Content(val contentUiState: SurveyContentUiState) : SurveyUiState()
    }

    var surveyUiState by mutableStateOf<SurveyUiState>(SurveyUiState.Summary(surveySummaryUiState))
        private set

    var highscoreUiState by mutableStateOf(HighscoreUiState(surveyConfig.score.leaderboard.limit, surveyConfig.score.leaderboard.showScores, emptyList()))
        private set

    private var currentPage = 0

    fun takeSurvey() {
        surveyUiState = SurveyUiState.Content(
            SurveyContentUiState(
                surveyTitle = surveyConfig.title,
                totalPages = surveyConfig.pages.size,
                currentPage = currentPage + 1,
                pageTitle = surveyConfig.pages[currentPage].title,
                pageDescription = surveyConfig.pages[currentPage].description,
                showQuestionScores = surveyConfig.score.showQuestionScores,
                content = surveyConfig.pages[currentPage].content
            )
        )
    }

    fun cancelSurvey() {
        resetSurvey()
    }

    private fun resetSurvey() {
        currentPage = 0
        surveyUiState = SurveyUiState.Summary(surveySummaryUiState)
    }

    fun advanceSurvey() {
        currentPage++

        //TODO checkInput()

        if (currentPage == surveyConfig.pages.size) { //finalized survey
            //TODO
            resetSurvey()
        } else { //advance to the next page
            surveyUiState = SurveyUiState.Content(
                (surveyUiState as SurveyUiState.Content).contentUiState.copy(
                    currentPage = currentPage + 1,
                    pageTitle = surveyConfig.pages[currentPage].title,
                    pageDescription = surveyConfig.pages[currentPage].description,
                    content = surveyConfig.pages[currentPage].content
                )
            )
        }
    }

    private fun checkInput() {

    }
}

data class HighscoreUiState(
    val limit: Int,
    val showScores: Boolean,
    val scores: List<HighscoreEntry>,
)

data class HighscoreEntry(
    val name: String,
    val score: Int,
)

data class SurveySummaryUiState(
    val title: String,
    val type: SurveyType,
    val totalPages: Int,
    val totalQuestions: Int,
    val description: String,
    val highscoreEnabled: Boolean = true,
)

data class SurveyContentUiState(
    val surveyTitle: String,
    val totalPages: Int,
    val currentPage: Int,
    val pageTitle: String? = null,
    val pageDescription: String? = null,
    val showQuestionScores: Boolean = false,
    val content: List<SurveyPageContent>,
)
