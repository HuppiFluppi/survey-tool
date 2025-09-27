package com.zinkel.survey.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import com.zinkel.survey.config.DataQuestionType
import com.zinkel.survey.config.SurveyConfig
import com.zinkel.survey.config.SurveyType
import com.zinkel.survey.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.highscore_unknown_player
import java.io.File
import java.time.ZonedDateTime

/**
 * Central state holder and controller for running a survey/quiz.
 *
 * Responsibilities:
 * - Exposes high-level UI states (summary vs. content) to the UI.
 * - Manages the lifecycle of a single survey run (start, navigate pages, validate, submit, cancel).
 * - Synchronizes UI input with the underlying [SurveyInstance] persisted via [SurveyDataManager].
 * - Maintains and updates a simple in-memory highscore list for quiz-type surveys.
 *
 * Threading model:
 * - Public APIs are designed to be called from the UI thread.
 * - IO/persistence and highscore update on completion happen on [coroutineScope].
 *
 * @param surveyConfig Parsed survey configuration from file, used to render pages.
 * @param configFile The file from which [surveyConfig] was loaded, used by [SurveyDataManager] for persistence.
 * @param coroutineScope Scope used for background work (saving results, updating highscores, etc.).
 */
class SurveyModel(private val surveyConfig: SurveyConfig, configFile: File, private val coroutineScope: CoroutineScope) {
    // surveySummaryUiState is initialized in the constructor as it is static for a survey
    private val surveySummaryUiState = SurveySummaryUiState(
        title = surveyConfig.title,
        type = surveyConfig.type,
        totalPages = surveyConfig.pages.size,
        totalQuestions = surveyConfig.pages.sumOf { it.content.size },
        description = surveyConfig.description,
        highscoreEnabled = surveyConfig.type == SurveyType.QUIZ && surveyConfig.score.showLeaderboard
    )

    // dataManager is responsible for persisting each survey run and a summary
    private val dataManager = SurveyDataManager(surveyConfig, configFile)

    // surveyInstance holds the information about a specific run/take of the survey
    private var surveyInstance: SurveyInstance? = null

    // surveyContentPage maps question ids to question for the current page of the survey
    private var surveyContentPage: Map<String, SurveyContentData> = emptyMap()

    /**
     * UI model representing the two major screens:
     * - [Summary]: Landing page with overview and optional leaderboard.
     * - [Content]: Active survey page with questions and validation feedback.
     */
    sealed class SurveyUiState {
        /** Summary/landing screen state. */
        data class Summary(val summaryUiState: SurveySummaryUiState) : SurveyUiState()

        /** Active survey page with content and input errors if any. */
        data class Content(val contentUiState: SurveyContentUiState) : SurveyUiState()
    }

    /**
     * Current high-level UI state.
     * - Initially set to [SurveyUiState.Summary].
     * - Switches to [SurveyUiState.Content] when [takeSurvey] is called.
     */
    var surveyUiState by mutableStateOf<SurveyUiState>(SurveyUiState.Summary(surveySummaryUiState))
        private set

    /**
     * Current highscore UI state. Only meaningful for quiz-type surveys.
     * Will be updated when a survey run is completed successfully.
     */
    var highscoreUiState by mutableStateOf(
        HighscoreUiState(
            surveyConfig.score.leaderboard.limit,
            surveyConfig.score.leaderboard.showScores,
            surveyConfig.score.leaderboard.showPlaceholder,
            emptyList()
        )
    )
        private set

    // Zero-based index
    private var currentPageIndex = 0

    init {
        coroutineScope.launch {
            loadPreviousData()
        }
    }

    /**
     * Loads previous data, initializing [SurveyDataManager] and get highscore data from previous survey instances.
     *
     * This method retrieves the top survey instances based on the leaderboard score limit defined in the survey configuration.
     * If no valid data is found, the method exits early. If data is available, it updates the highscore UI state
     * with the retrieved scores.
     */
    private suspend fun loadPreviousData() {
        val topScores = dataManager.fillSummaryAndInstanceIdFromPrevious(surveyConfig.score.leaderboard.limit)
        if (topScores.isEmpty()) return

        if (surveyConfig.type == SurveyType.QUIZ && surveyConfig.score.showLeaderboard) {
            val userPlaceholder = getString(Res.string.highscore_unknown_player)

            Snapshot.withMutableSnapshot {
                // as the UI sorts and limits, simply adding works but might eventually become a problem with many highscore entries
                highscoreUiState = highscoreUiState.copy(
                    scores = topScores.map {
                        HighscoreEntry(it.user ?: userPlaceholder, it.score ?: 0, it.endTime ?: ZonedDateTime.now())
                    }
                )
            }
        }
    }

    /**
     * Starts a new survey run:
     * - Creates a new [SurveyInstance].
     * - Populates the first page content and switches UI to [SurveyUiState.Content].
     * If a previous run was in progress and not submitted, its state is discarded.
     */
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

    /**
     * Cancels the current survey run and returns to the summary screen.
     * Any transient answers are discarded.
     */
    fun cancelSurvey() {
        resetSurvey()
    }

    /**
     * Navigates to the previous page of the survey.
     * - Synchronizes current page answers into the [SurveyInstance].
     * - Repopulates the previous page’s content.
     */
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

    /**
     * Resets the model to the summary screen, clearing transient state and reset page index.
     */
    private fun resetSurvey() {
        surveyInstance = null
        currentPageIndex = 0
        surveyUiState = SurveyUiState.Summary(surveySummaryUiState)
    }

    /**
     * Attempts to move forward in the survey:
     * - Validates the current page input; if invalid, shows errors and stops.
     * - If valid, stores answers, advances the page index.
     * - On completion (after last page), persists answers and updates the highscore asynchronously,
     *   then resets to the summary screen.
     */
    fun advanceSurvey() {
        // check input
        if (!checkInputValid()) return

        //input valid, advance
        syncPageToInstance()
        currentPageIndex++

        if (currentPageIndex == surveyConfig.pages.size) { //finalized survey
            val instance4save = surveyInstance ?: throw RuntimeException("SurveyInstance is null")
            coroutineScope.launch {
                completeInstance(instance4save)
                dataManager.addSurveyData(instance4save)
                updateHighscore(instance4save)
            }

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

    /**
     * Completes the survey instance. Sets user, score and endtime.
     *
     * The user is derived from the first NAME-type data question if present; otherwise a localized fallback is used.
     */
    private suspend fun completeInstance(instance: SurveyInstance) {
        val answers = instance.getAllAnswers()

        val user = answers.find { it is DataSurveyContentData && it.question.dataType == DataQuestionType.NAME && it.question.useForLeaderboard }
            ?.answer as? String ?: getString(Res.string.highscore_unknown_player)
        val score = answers.sumOf { it.calculateScore() }

        instance.user = user
        instance.score = score
        instance.endTime = ZonedDateTime.now()
    }

    /**
     * Adds a new highscore entry for the given [instance] and updates [highscoreUiState].
     *
     * Note: This operates in a mutable snapshot to avoid recomposition glitches.
     */
    private fun updateHighscore(instance: SurveyInstance) {
        if (surveyConfig.type != SurveyType.QUIZ || !surveyConfig.score.showLeaderboard) return

        Snapshot.withMutableSnapshot {
            // as the UI sorts and limits, simply adding works but might eventually become a problem with many highscore entries
            highscoreUiState = highscoreUiState.copy(scores = highscoreUiState.scores + HighscoreEntry(instance.user ?: "<unset>", instance.score ?: 0))
        }
    }

    /**
     * Validates all inputs on the current page and, if invalid, updates the UI with error messages.
     *
     * Implementation detail:
     * - Uses runBlocking to collect localized strings for validation errors (resource access is suspend).
     *   While not ideal, this keeps the call site synchronous for page navigation.
     *
     * @return true if all inputs on the page are valid; false otherwise.
     */
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

    /**
     * Builds the [surveyContentPage] map for the current page, optionally seeding content
     * with previously entered answers if the user navigated back.
     */
    private fun fillSurveyContentPage() {
        val previousAnswers = surveyInstance?.getPageAnswers(currentPageIndex)?.associate { it.question.id to it.answer }.orEmpty()
        surveyContentPage = surveyConfig.pages[currentPageIndex].content.associate {
            it.id to SurveyContentData.fromSurveyPageContent(it, previousAnswers[it.id])
        }
    }

    /**
     * Writes the current page content answers into the active [SurveyInstance].
     */
    private fun syncPageToInstance() {
        surveyInstance?.setPageAnswers(currentPageIndex, surveyContentPage.values.toList().filter { it.question.savable })
    }

    /**
     * Method to set an answer for the question with the given [id].
     *
     * @param id Question identifier.
     * @param answer The new textual answer value.
     */
    fun updateAnswer(id: String, answer: String) {
        when (val content = surveyContentPage[id]) {
            is DataSurveyContentData -> content.answer = answer
            is TextSurveyContentData -> content.answer = answer
            null                     -> throw RuntimeException("SurveyContentData <$id> not found")
            else                     -> throw RuntimeException("Unexpected content type: ${content::class}")
        }
    }

    /**
     * Method to set an answer for the question with the given [id].
     *
     * @param id Question identifier.
     * @param choices The new string list answer value.
     */
    fun updateAnswer(id: String, choices: List<String>) {
        (surveyContentPage[id] as? ChoiceSurveyContentData)?.answer = choices
    }

    /**
     * Method to set an answer for the question with the given [id].
     *
     * @param id Question identifier.
     * @param answer The new numeric answer value.
     */
    fun updateAnswer(id: String, answer: Int) {
        (surveyContentPage[id] as? RatingSurveyContentData)?.answer = answer
    }

    /**
     * Method to set an answer for the question with the given [id].
     *
     * @param id Question identifier.
     * @param statement The statement the answer is for.
     * @param choice The new answer value.
     */
    fun updateAnswer(id: String, statement: String, choice: String) {
        val content = surveyContentPage[id] as? LikertSurveyContentData
        if (content?.answer == null) content?.answer = mutableMapOf()
        content?.answer?.set(statement, choice)
    }

    /**
     * Method to set an answer for the question with the given [id].
     *
     * @param id Question identifier.
     * @param answer The new date/time answer value.
     */
    fun updateAnswer(id: String, answer: DateTimePick) {
        (surveyContentPage[id] as? DateTimeSurveyContentData)?.answer = answer
    }
}

/**
 * UI model for the leaderboard display.
 *
 * @param limit Maximum number of entries to display.
 * @param showScores Whether to show numeric scores next to names.
 * @param showPlaceholder Whether empty lines should be filled up to limit.
 * @param scores Current list of entries, newest appended at the end.
 */
data class HighscoreUiState(
    val limit: Int,
    val showScores: Boolean,
    val showPlaceholder: Boolean,
    val scores: List<HighscoreEntry>,
)

/**
 * A single leaderboard entry.
 *
 * @param name Display name of the participant.
 * @param score Score of participant.
 */
data class HighscoreEntry(
    val name: String,
    val score: Int,
    val time: ZonedDateTime = ZonedDateTime.now(),
)

/**
 * Summary information shown on the survey landing page.
 *
 * @param title Survey title.
 * @param type Survey type (e.g. QUIZ).
 * @param totalPages Number of pages in the survey.
 * @param totalQuestions Total number of questions across all pages.
 * @param description Survey description.
 * @param highscoreEnabled Whether the leaderboard should be shown on the summary screen (for quizzes).
 */
data class SurveySummaryUiState(
    val title: String,
    val type: SurveyType,
    val totalPages: Int,
    val totalQuestions: Int,
    val description: String,
    val highscoreEnabled: Boolean = true,
)

/**
 * UI model representing the content of a single survey page.
 *
 * @param surveyTitle Title of the survey for context.
 * @param totalPages Total number of pages.
 * @param currentPage One-based index of the currently displayed page.
 * @param pageTitle Optional title of the current page.
 * @param pageDescription Optional description of the current page.
 * @param showQuestionScores Whether to show per-question scores (used in quizzes).
 * @param content Renderable content blocks/questions for this page.
 * @param inputErrors Mapping from question id to a user-facing error message.
 */
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
