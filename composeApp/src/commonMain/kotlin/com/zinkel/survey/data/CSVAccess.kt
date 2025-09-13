package com.zinkel.survey.data

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.zinkel.survey.config.SurveyType
import java.io.File

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
            if (instance.type == SurveyType.QUIZ) add(instance.getAllAnswers().sumOf { it.calculateScore() })
            instance.getAllAnswers().forEach { answer ->
                add(answer.question.id)
                add(answer.question.title)
                add(answer.answer)
            }
        }

    private fun generateHeaderRow(questionCount: Int, instance: SurveyInstance): List<String> = buildList {
        add("#")
        add("Start Time")
        add("End Time")
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
}
