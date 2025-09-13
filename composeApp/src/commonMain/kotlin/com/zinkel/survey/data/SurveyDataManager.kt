package com.zinkel.survey.data

import com.zinkel.survey.config.SurveyConfig
import com.zinkel.survey.config.SurveyType
import java.io.File
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

class SurveyDataManager(surveyConfig: SurveyConfig, surveyFile: File, formatType: String = "csv") {

    private val dataAccess: SurveyDataAccess = accessTypes[formatType] ?: throw IllegalArgumentException("Unsupported file format: $formatType")
    private val summaryFile = File(surveyFile.parent, surveyFile.nameWithoutExtension + SUMMARY_FILE_SUFFIX)
    private val dataFile = File(surveyFile.parent, surveyFile.nameWithoutExtension + DATA_FILE_SUFFIX)

    private val summary = SurveySummary(
        title = surveyConfig.title,
        type = surveyConfig.type,
        pageCount = surveyConfig.pages.size,
        questionCount = surveyConfig.pages.sumOf { it.content.size })

    private suspend fun updateSummary(instance: SurveyInstance) {
        summary.submittedCount++
        summary.firstSubmittedTime ?: run { summary.firstSubmittedTime = ZonedDateTime.now() }
        summary.lastSubmittedTime = ZonedDateTime.now()
        if (summary.type == SurveyType.QUIZ) {
            val score = instance.getAllAnswers().sumOf { it.calculateScore() }
            summary.minScore = min(summary.minScore ?: Int.MAX_VALUE, score)
            summary.maxScore = max(summary.maxScore ?: Int.MIN_VALUE, score)
        }

        dataAccess.saveSurveySummary(summaryFile, summary)
    }

    suspend fun addSurveyData(instance: SurveyInstance) {
        if (instance.endTime == null) {
            instance.endTime = ZonedDateTime.now()
        }

        updateSummary(instance)

        dataAccess.saveSurveyData(dataFile, instance)
    }

    private var instanceId = 1
    fun newSurveyInstance() = SurveyInstance(instanceId++, summary.type)

    companion object {
        const val SUMMARY_FILE_SUFFIX = "_summary"
        const val DATA_FILE_SUFFIX = "_data"

        private val accessTypes = mutableMapOf<String, SurveyDataAccess>()

        init {
            accessTypes["csv"] = CSVAccess()
        }
    }
}

interface SurveyDataAccess {
    suspend fun saveSurveyData(file: File, instance: SurveyInstance)
    suspend fun saveSurveySummary(file: File, summary: SurveySummary)
}

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

class SurveyInstance(
    val instanceId: Int,
    val type: SurveyType = SurveyType.SURVEY,
    val startTime: ZonedDateTime = ZonedDateTime.now(),
    var endTime: ZonedDateTime? = null,
) {
    private val pageAnswers = mutableMapOf<Int, List<SurveyContentData>>()

    fun setPageAnswers(pageIndex: Int, answers: List<SurveyContentData>) {
        pageAnswers[pageIndex] = answers
    }

    fun getPageAnswers(pageNumber: Int): List<SurveyContentData>? {
        return pageAnswers[pageNumber]
    }

    fun getAllAnswers() = pageAnswers.values.flatten()
}
