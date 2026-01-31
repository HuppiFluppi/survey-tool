package com.zinkel.survey.data

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.zinkel.survey.config.SurveyType
import java.io.File
import java.time.ZonedDateTime

class CSVAccess : SurveyDataAccess {

    override suspend fun saveSurveyData(file: File, instance: SurveyInstance) {
        val outputFile = File(file.parent, file.name + ".csv")
        val writeHeader = !outputFile.exists() || outputFile.length() == 0L

        csvWriter().openAsync(outputFile, append = true) {
            if (writeHeader) { //add header row
                writeRow(generateHeaderRow(instance.getAllAnswers().size, instance))
            }
            writeRow(generateDataRow(instance))
        }
    }

    private fun generateDataRow(instance: SurveyInstance): List<Any?> =
        buildList {
            add(instance.instanceId)
            add(instance.startTime)
            add(instance.endTime)
            add(instance.type)
            if (instance.type == SurveyType.QUIZ) add(sanitizeCSV(instance.user))
            if (instance.type == SurveyType.QUIZ) add(instance.score)
            instance.getAllAnswers().forEach { answer ->
                add(answer.question.id)
                add(answer.question.title)
                add(sanitizeCSV(answer.answer))
            }
        }

    // sanitize CSV data to prevent formula injection
    private fun sanitizeCSV(value: Any?): Any? {
        if (value == null) return null
        if (value !is String) return value

        val trimmed = value.trim()
        if (trimmed.isEmpty()) return trimmed

        // Escape if starts with dangerous characters
        return if (trimmed.firstOrNull() in setOf('=', '+', '-', '@', '\t', '\r')) {
            "'$trimmed"  // Prefix with single quote to treat as text
        } else {
            trimmed
        }
    }

    private fun generateHeaderRow(questionCount: Int, instance: SurveyInstance): List<String> = buildList {
        add("#")
        add("Start Time")
        add("End Time")
        add("Survey Type")
        if (instance.type == SurveyType.QUIZ) add("User")
        if (instance.type == SurveyType.QUIZ) add("Score")
        repeat(questionCount) {
            add("Question ID")
            add("Question Title")
            add("Question Answer")
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
            readAllWithHeaderAsSequence().map { row ->
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
            }.filterNotNull().toList()
        }
        return instances
    }
}
