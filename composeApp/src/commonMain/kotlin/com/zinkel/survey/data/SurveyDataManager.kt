package com.zinkel.survey.data

import com.zinkel.survey.config.SurveyConfig
import com.zinkel.survey.config.SurveyType
import com.zinkel.survey.data.SurveyDataManager.Companion.DATA_FILE_SUFFIX
import com.zinkel.survey.data.SurveyDataManager.Companion.SUMMARY_FILE_SUFFIX
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Coordinates persistence of survey runs and summary statistics.
 *
 * Responsibilities:
 * - Provides an abstraction over concrete persistence implementations via [SurveyDataAccess].
 * - Writes a compact per-run dataset (answers, timestamps) as well as an aggregated [SurveySummary].
 * - Tracks instance IDs for new survey runs.
 *
 * Files:
 * - Summary file: same base name as the source survey file plus [SUMMARY_FILE_SUFFIX].
 * - Data file: same base name plus [DATA_FILE_SUFFIX].
 *
 * Threading:
 * - All write operations are suspend functions of [SurveyDataAccess]; callers should invoke them
 *   on a background dispatcher if needed.
 *
 * @param surveyConfig Parsed configuration used to derive static summary fields (page/question counts, type).
 * @param surveyFile The source file of the survey definition; used to place alongside summary/data files.
 * @param formatType Logical storage format key resolving to a [SurveyDataAccess] implementation (default "csv").
 * @throws IllegalArgumentException when [formatType] is not supported.
 */
class SurveyDataManager(surveyConfig: SurveyConfig, surveyFile: File, formatType: String = "csv") {

    private val dataAccess: SurveyDataAccess = accessTypes[formatType] ?: throw IllegalArgumentException("Unsupported file format: $formatType")
    private val summaryFile = File(surveyFile.parent, surveyFile.nameWithoutExtension + SUMMARY_FILE_SUFFIX)
    private val dataFile = File(surveyFile.parent, surveyFile.nameWithoutExtension + DATA_FILE_SUFFIX)

    private val summary = SurveySummary(
        title = surveyConfig.title,
        type = surveyConfig.type,
        pageCount = surveyConfig.pages.size,
        questionCount = surveyConfig.pages.sumOf { it.content.size })

    /**
     * Updates in-memory [summary] based on the completed [instance] and persists it.
     *
     * For QUIZ types, also keeps track of min/max scores observed so far.
     */
    private suspend fun updateSummary(instance: SurveyInstance) {
        summary.submittedCount++
        summary.firstSubmittedTime ?: run { summary.firstSubmittedTime = ZonedDateTime.now() }
        summary.lastSubmittedTime = ZonedDateTime.now()
        if (summary.type == SurveyType.QUIZ) {
            val score = instance.score ?: 0
            summary.minScore = min(summary.minScore ?: Int.MAX_VALUE, score)
            summary.maxScore = max(summary.maxScore ?: Int.MIN_VALUE, score)
        }

        dataAccess.saveSurveySummary(summaryFile, summary)
    }

    /**
     * Tries to load previous survey runs ([SurveyInstance]s) from file to update the instanceId and summary.
     *
     * This method should be called once after creation, before any survey instances are created (via [newSurveyInstance]).
     *
     * @param limit Maximum number of instances to return; if there are fewer available, all will be returned.
     * @return List of [SurveyInstance]s with highest score.
     */
    suspend fun fillSummaryAndInstanceIdFromPrevious(limit: Int): List<SurveyInstance> {
        val list = dataAccess.loadHeadSurveyData(dataFile)
        if (list.isEmpty()) return emptyList()

        val submitCount: Int = list.size
        var previousId = 0
        var previousFirstTime: ZonedDateTime = ZonedDateTime.now()
        val zeroTimeToken = Instant.ofEpochMilli(0).atZone(ZoneId.of("UTC"))
        var previousLastTime: ZonedDateTime = zeroTimeToken
        var previousHighScore: Int = Int.MIN_VALUE
        var previousLowScore: Int = Int.MAX_VALUE

        val topInstances = PriorityQueue<SurveyInstance>(limit + 1, compareBy { it.score ?: 0 })

        list.forEach {
            previousId = max(previousId, it.instanceId)

            if (it.score != null) {
                previousHighScore = max(previousHighScore, it.score!!)
                previousLowScore = min(previousLowScore, it.score!!)
            }

            previousFirstTime = if (it.startTime.isBefore(previousFirstTime)) it.startTime else previousFirstTime
            previousLastTime = if (it.endTime?.isAfter(previousLastTime) == true) it.endTime!! else previousLastTime

            topInstances.add(it)
            if (topInstances.size > limit) topInstances.poll()
        }

        instanceId = previousId
        summary.submittedCount = submitCount
        summary.firstSubmittedTime = previousFirstTime
        summary.lastSubmittedTime = if (previousLastTime.isEqual(zeroTimeToken)) ZonedDateTime.now() else previousLastTime
        summary.maxScore = if (previousHighScore == Int.MIN_VALUE) 0 else previousHighScore
        summary.minScore = if (previousLowScore == Int.MAX_VALUE) 0 else previousLowScore

        return topInstances.toList()
    }

    /**
     * Persists a completed survey [instance].
     *
     * Behavior:
     * - Updates and saves the aggregated [SurveySummary].
     * - Saves per-run data via the configured [SurveyDataAccess].
     */
    suspend fun addSurveyData(instance: SurveyInstance) {
        updateSummary(instance)
        dataAccess.saveSurveyData(dataFile, instance)
    }

    private var instanceId = 0

    /**
     * Creates a new [SurveyInstance] with a unique id and the current survey type.
     */
    fun newSurveyInstance() = SurveyInstance(++instanceId, summary.type)

    companion object {
        /** Suffix appended to the base survey file name for the summary file. */
        const val SUMMARY_FILE_SUFFIX = "_summary"

        /** Suffix appended to the base survey file name for the data file. */
        const val DATA_FILE_SUFFIX = "_data"

        private val accessTypes = mutableMapOf<String, SurveyDataAccess>()

        init {
            accessTypes["csv"] = CSVAccess()
        }
    }
}

/**
 * Interface for persisting survey data and summaries.
 *
 * Implementations decide on file formats and schemas. All methods are suspend to
 * support non-blocking IO.
 */
interface SurveyDataAccess {
    /**
     * Saves an entire survey [instance]/run (answers, metadata) to [file].
     */
    suspend fun saveSurveyData(file: File, instance: SurveyInstance)

    /**
     * Saves the aggregated [summary] to [file].
     */
    suspend fun saveSurveySummary(file: File, summary: SurveySummary)

    /**
     * Loads the header(w/o questions) survey instance data from file [file].
     */
    suspend fun loadHeadSurveyData(file: File): List<SurveyInstance>
}

/**
 * Aggregated, append-only summary computed across all submitted runs of a survey.
 *
 * @property title Survey title (copied from configuration for convenience).
 * @property type Survey type to distinguish SURVEY vs QUIZ behavior.
 * @property pageCount Number of pages in the survey.
 * @property questionCount Total number of questions across all pages.
 * @property submittedCount Total number of completed survey runs.
 * @property firstSubmittedTime Timestamp of the first completed run.
 * @property lastSubmittedTime Timestamp of the latest completed run.
 * @property minScore Minimum score observed across runs (QUIZ only).
 * @property maxScore Maximum score observed across runs (QUIZ only).
 */
data class SurveySummary(
    val title: String,
    val type: SurveyType,
    val pageCount: Int,
    val questionCount: Int,
    var submittedCount: Int = 0,
    var firstSubmittedTime: ZonedDateTime? = null,
    var lastSubmittedTime: ZonedDateTime? = null,
    var minScore: Int? = null,
    var maxScore: Int? = null,
)

/**
 * Represents a single user’s run through the survey.
 *
 * Holds timestamps and a per-page mapping of answers. Answers are stored as a list
 * of [SurveyContentData] to preserve association with question metadata and scoring.
 *
 * @property instanceId Unique, monotonically increasing identifier within the current process.
 * @property type Survey type; used e.g. for scoring behavior.
 * @property startTime Timestamp when the run started.
 * @property endTime Timestamp when the run ended; null until completion.
 * @property score The score (only for QUIZ type) of the run; null until completion.
 */
class SurveyInstance(
    val instanceId: Int,
    val type: SurveyType = SurveyType.SURVEY,
    val startTime: ZonedDateTime = ZonedDateTime.now(),
    var endTime: ZonedDateTime? = null,
    var user: String? = null,
    var score: Int? = null,
) {
    private val pageAnswers = mutableMapOf<Int, List<SurveyContentData>>()

    /**
     * Replaces the stored answers for a specific page.
     *
     * @param pageIndex Zero-based page index.
     * @param answers Ordered list of content/answers for that page.
     */
    fun setPageAnswers(pageIndex: Int, answers: List<SurveyContentData>) {
        pageAnswers[pageIndex] = answers
    }

    /**
     * Returns the answers for a given page, if any.
     *
     * @param pageNumber Zero-based page index.
     */
    fun getPageAnswers(pageNumber: Int): List<SurveyContentData>? {
        return pageAnswers[pageNumber]
    }

    /**
     * Flattens and returns all answers across all pages in navigation order.
     */
    fun getAllAnswers() = pageAnswers.values.flatten()
}
