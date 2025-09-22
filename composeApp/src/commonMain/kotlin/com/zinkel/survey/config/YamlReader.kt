package com.zinkel.survey.config

import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.io.path.Path

class YamlReader : SurveyConfigReader {

    private val yamlLoadSettings: LoadSettings = LoadSettings.builder().build()

    override fun loadConfig(file: File): SurveyConfig {
        return YamlProcessor(file).load()
    }

    private inner class YamlProcessor(private val inputFile: File) {

        fun load(): SurveyConfig {
            val load = Load(yamlLoadSettings)
            val documents = inputFile.inputStream().use { stream ->
                load.loadAllFromInputStream(stream).map { it as Map<String, Any> }
            }

            if (documents.isEmpty() || documents.size < 2) throw IllegalArgumentException("Survey file malformed (not enough documents)")

            return mapYamlToSurveyConfig(documents)
        }

        private fun mapYamlToSurveyConfig(documents: List<Map<String, Any>>): SurveyConfig {
            return mapFirstDocument(documents.first()).copy(pages = documents.drop(1).mapIndexed { index, it -> mapPage(it, index) })
        }

        private fun mapFirstDocument(document: Map<String, Any>) = SurveyConfig(
            title = document["title"] as? String ?: throw IllegalArgumentException("Survey file malformed (no title)"),
            description = document["description"] as? String ?: throw IllegalArgumentException("Survey file malformed (no description)"),
            type = (document["type"] as? String)?.let { SurveyType.valueOf(it.uppercase()) } ?: SurveyType.SURVEY,
            score = (document["score"] as? Map<String, Any>)?.let { mapScore(it) } ?: ScoreSettings(),
            pages = emptyList()
        )

        private fun mapScore(score: Map<String, Any>) = ScoreSettings(
            showQuestionScores = score["show_question_scores"] as? Boolean ?: false,
            showLeaderboard = score["show_leaderboard"] as? Boolean ?: true,
            leaderboard = (score["leaderboard"] as? Map<String, Any>)?.let { mapLeaderboard(it) } ?: LeaderboardSettings()
        )

        private fun mapLeaderboard(leaderboard: Map<String, Any>) = LeaderboardSettings(
            showScores = leaderboard["show_scores"] as? Boolean ?: true,
            limit = leaderboard["limit"] as? Int ?: 10,
        )

        private fun mapPage(document: Map<String, Any>, pageNumber: Int) = SurveyPage(
            title = document["title"] as? String,
            description = document["description"] as? String,
            content = (document["content"] as? List<Map<String, Any>>)?.mapIndexed { index, it -> mapContent(it, pageNumber, index) }
                ?: throw IllegalArgumentException("Survey file malformed (document without content)")
        )

        private fun mapContent(content: Map<String, Any>, pageNumber: Int, contentNumber: Int): SurveyPageContent {
            val type = (content["type"] as? String)?.let { SurveyContentType.valueOf(it.uppercase()) }
                ?: throw IllegalArgumentException("Survey file malformed (content without type)")
            val title = content["title"] as? String ?: throw IllegalArgumentException("Survey file malformed (no title for content)")
            val required = content["required"] as? Boolean ?: true

            when (type) {
                SurveyContentType.TEXT        -> {
                    val config = content["config"] as? Map<String, Any>
                    return TextQuestion(
                        title = title,
                        id = getContentId(pageNumber, contentNumber),
                        required = required,

                        multiline = config?.get("multiline") as? Boolean ?: false,
                        score = config?.get("score") as? Int,
                        correctAnswer = config?.get("correct_answer") as? String,
                    )
                }

                SurveyContentType.CHOICE      -> {
                    val config =
                        content["config"] as? Map<String, Any> ?: throw IllegalArgumentException("Survey file malformed (no config for choice content)")
                    return ChoiceQuestion(
                        title = title,
                        id = getContentId(pageNumber, contentNumber),
                        required = required,

                        multiple = config["multiple"] as? Boolean ?: false,
                        limit = config["limit"] as? Int ?: 2,
                        choices = (config["choices"] as? List<Map<String, Any>>)?.map {
                            ChoiceItem(
                                title = it["title"] as? String ?: throw IllegalArgumentException("Survey file malformed (no title for choice content)"),
                                score = it["score"] as? Int,
                                correct = it["correct"] as? Boolean ?: false,
                            )
                        } ?: throw IllegalArgumentException("Survey file malformed (no choices for choice content)")
                    )
                }

                SurveyContentType.DATA        -> {
                    val config = content["config"] as? Map<String, Any>
                    return DataQuestion(
                        title = title,
                        id = getContentId(pageNumber, contentNumber),
                        required = required,

                        dataType = (config?.get("data_type") as? String)?.let { DataQuestionType.valueOf(it.uppercase()) } ?: DataQuestionType.NAME,
                        validationPattern = config?.get("validation_pattern") as? String,
                        useForLeaderboard = config?.get("use_for_leaderboard") as? Boolean ?: true,
                    )
                }

                SurveyContentType.RATING      -> {
                    return RatingQuestion(
                        title = title,
                        id = getContentId(pageNumber, contentNumber),
                        required = required,
                    )
                }

                SurveyContentType.LIKERT      -> {
                    val config =
                        content["config"] as? Map<String, Any> ?: throw IllegalArgumentException("Survey file malformed (no config for likert content)")
                    return LikertQuestion(
                        title = title,
                        id = getContentId(pageNumber, contentNumber),
                        required = required,

                        choices = (config["choices"] as? List<String>)
                            ?: throw IllegalArgumentException("Survey file malformed (no choices for likert content)"),
                        statements = (config["statements"] as? List<Map<String, Any>>)?.map {
                            LikertStatement(
                                title = it["title"] as? String ?: throw IllegalArgumentException("Survey file malformed (no title for likert statement)"),
                                score = it["score"] as? Int,
                                correctChoice = it["correct_choice"] as? String,
                            )
                        } ?: throw IllegalArgumentException("Survey file malformed (no statements for likert content)")
                    )
                }

                SurveyContentType.INFORMATION -> {
                    return InformationBlock(
                        title = title,
                        id = getContentId(pageNumber, contentNumber),
                        description = content["description"] as? String,
                        image = checkFile(content["image_path"] as? String, title, type),
                    )
                }

                SurveyContentType.DATETIME   -> {
                    val config = content["config"] as? Map<String, Any>

                    return DateTimeQuestion(
                        title = title,
                        id = getContentId(pageNumber, contentNumber),
                        required = required,

                        inputType = (config?.get("input_type") as? String)?.let { DateTimeType.valueOf(it.uppercase()) } ?: DateTimeType.DATETIME,
                        initialSelectedTime = (config?.get("initial_selected_time") as? String)?.let { LocalTime.parse(it) },
                        initialSelectedDate = (config?.get("initial_selected_date") as? String)?.let { LocalDate.parse(it) },

                        score = config?.get("score") as? Int,
                        correctTimeAnswer = (config?.get("correct_time_answer") as? String)?.let { LocalTime.parse(it) },
                        correctDateAnswer = (config?.get("correct_date_answer") as? String)?.let { LocalDate.parse(it) },
                    )
                }
            }
        }

        private fun checkFile(filePath: String?, title: String, type: SurveyContentType): File? {
            if (filePath == null) return null

            // find file
            val file = File(filePath).takeIf { it.isAbsolute } // absolute path
                ?: Path(inputFile.parent, filePath).toFile() // path relative to input file
            if (!file.exists()) throw IllegalArgumentException("Title: '$title' of type $type. File not found: $filePath")

            // check file
            if (file.length() > 10_485_760) throw IllegalArgumentException("Title: '$title' of type $type. File exceeds 10mb: $filePath")

            return file
        }

    }
}
