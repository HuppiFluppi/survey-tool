package com.zinkel.survey.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zinkel.survey.config.SurveyConfig
import com.zinkel.survey.config.SurveyType
import com.zinkel.survey.data.ChoiceSurveyContentData
import com.zinkel.survey.data.LikertSurveyContentData
import com.zinkel.survey.data.NameSurveyContentData
import com.zinkel.survey.data.RatingSurveyContentData
import com.zinkel.survey.data.SurveyContentData
import com.zinkel.survey.data.SurveyDataManager
import com.zinkel.survey.data.SurveyInstance
import com.zinkel.survey.data.TextSurveyContentData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import java.io.File

class SurveyModel(private val surveyConfig: SurveyConfig, configFile: File, private val coroutineScope: CoroutineScope) {
    private val surveySummaryUiState = SurveySummaryUiState(
        title = surveyConfig.title,
        type = surveyConfig.type,
        totalPages = surveyConfig.pages.size,
        totalQuestions = surveyConfig.pages.sumOf { it.content.size },
        description = surveyConfig.description,
        highscoreEnabled = surveyConfig.type == SurveyType.QUIZ && surveyConfig.score.showLeaderboard
    )

    private val dataManager = SurveyDataManager(surveyConfig, configFile)
    private var surveyInstance: SurveyInstance? = null
    private var surveyContentPage: Map<String, SurveyContentData> = emptyMap()

    sealed class SurveyUiState {
        data class Summary(val summaryUiState: SurveySummaryUiState) : SurveyUiState()
        data class Content(val contentUiState: SurveyContentUiState) : SurveyUiState()
    }

    var surveyUiState by mutableStateOf<SurveyUiState>(SurveyUiState.Summary(surveySummaryUiState))
        private set

    var highscoreUiState by mutableStateOf(HighscoreUiState(surveyConfig.score.leaderboard.limit, surveyConfig.score.leaderboard.showScores, emptyList()))
        private set

    private var currentPageIndex = 0

    fun takeSurvey() {
        surveyInstance = dataManager.newSurveyInstance()
        fillSurveyContentPage()
        surveyUiState = SurveyUiState.Content(
            SurveyContentUiState(
                surveyTitle = surveyConfig.title,
                totalPages = surveyConfig.pages.size,
                currentPage = currentPageIndex + 1,
                pageTitle = surveyConfig.pages[currentPageIndex].title,
                pageDescription = surveyConfig.pages[currentPageIndex].description,
                showQuestionScores = surveyConfig.score.showQuestionScores,
                content = surveyContentPage.values.toList(),
            )
        )
    }

    fun cancelSurvey() {
        resetSurvey()
    }

    fun backSurvey() {
        if (currentPageIndex == 0) return //should be prevented by UI logic

        syncPageToInstance()
        currentPageIndex--

        // set content
        fillSurveyContentPage()
        surveyUiState = SurveyUiState.Content(
            (surveyUiState as SurveyUiState.Content).contentUiState.copy(
                currentPage = currentPageIndex + 1,
                pageTitle = surveyConfig.pages[currentPageIndex].title,
                pageDescription = surveyConfig.pages[currentPageIndex].description,
                content = surveyContentPage.values.toList(),
            )
        )
    }

    private fun resetSurvey() {
        surveyInstance = null
        currentPageIndex = 0
        surveyUiState = SurveyUiState.Summary(surveySummaryUiState)
    }

    fun advanceSurvey() {
        // check input
        if (!checkInputValid()) return

        //input valid, advance
        syncPageToInstance()
        currentPageIndex++

        if (currentPageIndex == surveyConfig.pages.size) { //finalized survey
            val instance4save = surveyInstance ?: throw RuntimeException("SurveyInstance is null")
            coroutineScope.launch {
                dataManager.addSurveyData(instance4save)
            }
            //TODO highscore
            resetSurvey()
        } else { //advance to the next page
            fillSurveyContentPage()
            surveyUiState = SurveyUiState.Content(
                (surveyUiState as SurveyUiState.Content).contentUiState.copy(
                    currentPage = currentPageIndex + 1,
                    pageTitle = surveyConfig.pages[currentPageIndex].title,
                    pageDescription = surveyConfig.pages[currentPageIndex].description,
                    content = surveyContentPage.values.toList(),
                    inputErrors = emptyMap()
                )
            )
        }
    }

    private fun checkInputValid(): Boolean {
        val inputErrors = runBlocking { //suspending needed to get resource strings. while not ideal, runBlocking is chosen here
            surveyContentPage.entries.asFlow()
                .map { it.key to it.value.validate() }
                .filterNot { it.second.isValid }
                .map {
                    val errors = it.second.validationErrors.orEmpty().map { error -> getString(error) }
                    it.first to errors.joinToString()
                }.toList()
                .associate { it.first to it.second }
        }

        if (inputErrors.isNotEmpty()) {
            surveyUiState = SurveyUiState.Content(
                (surveyUiState as SurveyUiState.Content).contentUiState.copy(
                    inputErrors = inputErrors
                )
            )
            return false
        }

        return true
    }

    private fun fillSurveyContentPage() {
        val previousAnswers = surveyInstance?.getPageAnswers(currentPageIndex)?.associate { it.question.id to it.answer }.orEmpty()
        surveyContentPage = surveyConfig.pages[currentPageIndex].content.associate {
            it.id to SurveyContentData.fromSurveyPageContent(it, previousAnswers[it.id])
        }
    }

    private fun syncPageToInstance() {
        surveyInstance?.setPageAnswers(currentPageIndex, surveyContentPage.values.toList())
    }

    fun updateAnswer(id: String, answer: String) {
        when (val content = surveyContentPage[id]) {
            is NameSurveyContentData -> content.answer = answer
            is TextSurveyContentData -> content.answer = answer
            null                     -> throw RuntimeException("SurveyContendData <$id> not found")
            else                     -> throw RuntimeException("Unexpected content type: ${content::class}")
        }
    }

    fun updateAnswer(id: String, choices: List<String>) {
        (surveyContentPage[id] as? ChoiceSurveyContentData)?.answer = choices
    }

    fun updateAnswer(id: String, answer: Int) {
        (surveyContentPage[id] as? RatingSurveyContentData)?.answer = answer
    }

    fun updateAnswer(id: String, statement: String, choice: String) {
        val content = surveyContentPage[id] as? LikertSurveyContentData
        if (content?.answer == null) content?.answer = mutableMapOf()
        content?.answer?.set(statement, choice)
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
    val content: List<SurveyContentData>,
    val inputErrors: Map<String, String> = emptyMap(),
)
