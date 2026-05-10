package com.zinkel.survey.data

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.zinkel.survey.config.SurveyType
import java.io.File
import java.time.ZonedDateTime

object CSVAccess : SurveyDataAccess {

    override suspend fun saveSurveyData(
        file: File,
        instance: SurveyInstance,
        totalPages: Int,
        hasConditionals: Boolean,
        questionIDandTitles: List<Pair<String, String>>,
    ) {
        val outputFile = File(file.parent, file.name + ".csv")
        val writeHeader = !outputFile.exists() || outputFile.length() == 0L

        csvWriter().openAsync(outputFile, append = true) {
            if (writeHeader) { //add header row
                writeRow(generateHeaderRow(questionIDandTitles, instance, hasConditionals))
            }
            writeRow(generateDataRow(instance, hasConditionals, totalPages, questionIDandTitles.size, questionIDandTitles))
        }
    }

    private fun generateHeaderRow(questionIDandTitles: List<Pair<String, String>>, instance: SurveyInstance, hasConditionals: Boolean): List<String> =
        buildList {
            add("#")
            add("Start Time")
            add("End Time")
            add("Survey Type")
            if (instance.type == SurveyType.QUIZ) add("User")
            if (instance.type == SurveyType.QUIZ) add("Score")
            if (hasConditionals) {
                add("Shown Pages")
                add("Shown Questions")
            }
            addAll(questionIDandTitles.map { it.second })
        }

    private fun generateDataRow(
        instance: SurveyInstance,
        hasConditionals: Boolean,
        totalPages: Int,
        totalQuestions: Int,
        questionIDandTitles: List<Pair<String, String>>,
    ): List<Any?> {
        val answers = instance.getAllAnswers().associateBy { it.question.id }

        return buildList {
            add(instance.instanceId)
            add(instance.startTime)
            add(instance.endTime)
            add(instance.type)
            if (instance.type == SurveyType.QUIZ) add(sanitizeCSV(instance.user))
            if (instance.type == SurveyType.QUIZ) add(instance.score)
            if (hasConditionals) {
                add("${instance.amountOfPages}/$totalPages")
                add("${instance.amountOfQuestions}/$totalQuestions")
            }
            questionIDandTitles.forEach { entry ->
                when (val data = answers[entry.first]) {
                    null -> add("")
                    // Specialization when the slider only has one value (not a range)
                    is SliderSurveyContentData if data.answer != null && data.answer?.second == null -> add(data.answer?.first)
                    // Specialization when there is only one choice
                    is ChoiceSurveyContentData if !data.question.multiple && data.answer?.size == 1 -> add(sanitizeCSV(data.answer?.first()))
                    else -> add(sanitizeCSV(data.answer))
                }
            }
        }
    }

    // sanitize CSV data to prevent formula injection
    private fun sanitizeCSV(value: Any?): Any? {
        if (value == null) return null
        if (value !is String) return value

        val trimmed = value.trim()
        if (trimmed.isEmpty()) return trimmed

        // Escape if starts with dangerous characters
        return if (trimmed.firstOrNull() in setOf('=', '+', '@', '\t', '\r')) {
            "'$trimmed"
        } else if (trimmed.startsWith('-') && trimmed.getOrNull(1)?.isDigit() != true) {
            "'$trimmed"  // Only escape '-' when not followed by a digit (i.e., not a negative number)
        } else {
            trimmed
        }
    }

    override suspend fun saveSurveySummary(file: File, summary: SurveySummary) {
        val outputFile = File(file.parent, file.name + ".csv")

        csvWriter().openAsync(outputFile, append = false) {
            // header row
            writeRow(
                buildList {
                    add("Survey Title")
                    add("Survey Type")
                    add("Page Count")
                    add("Question Count")
                    add("Submitted Count")
                    add("First Submit Time")
                    add("Last Submit Time")
                    if (summary.type == SurveyType.QUIZ) {
                        add("Min Score")
                        add("Max Score")
                    }
                }
            )

            // summary row
            writeRow(
                buildList {
                    add(summary.title)
                    add(summary.type)
                    add(summary.pageCount)
                    add(summary.questionCount)
                    add(summary.submittedCount)
                    add(summary.firstSubmittedTime)
                    add(summary.lastSubmittedTime)
                    if (summary.type == SurveyType.QUIZ) {
                        add(summary.minScore)
                        add(summary.maxScore)
                    }
                }
            )
        }
    }

    override suspend fun loadHeadSurveyData(file: File): List<SurveyInstance> {
        val inputFile = File(file.parent, file.name + ".csv")
        if (!inputFile.exists()) return emptyList()

        val instances = csvReader { autoRenameDuplicateHeaders = true; skipEmptyLine = true }.openAsync(inputFile) {
            readAllWithHeaderAsSequence().mapNotNull { row ->
                try {
                    SurveyInstance(
                        instanceId = row["#"]?.toIntOrNull() ?: throw IllegalArgumentException("Survey data file malformed (no # or wrong format)"),
                        startTime = row["Start Time"]?.let { ZonedDateTime.parse(it) }
                            ?: throw IllegalArgumentException("Survey data file malformed (no startTime or wrong format)"),
                        endTime = row["End Time"]?.let { ZonedDateTime.parse(it) },
                        type = SurveyType.valueOf(row["Survey Type"]?.uppercase() ?: throw IllegalArgumentException("Survey data file malformed (no type)")),
                        score = row["Score"]?.toIntOrNull(),
                        user = row["User"]
                    )
                } catch (e: Exception) {
                    println("Error loading survey data: ${e.message}")
                    null
                }
            }.toList()
        }
        return instances
    }
}
