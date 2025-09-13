package com.zinkel.survey.config

//root class for a survey config
data class SurveyConfig(
    val title: String,
    val description: String,
    val type: SurveyType = SurveyType.SURVEY,
    val imagePath: String? = null,

    val score: ScoreSettings = ScoreSettings(),

    val pages: List<SurveyPage>,
)

enum class SurveyType {
    SURVEY,
    QUIZ
}

data class ScoreSettings(
    val showQuestionScores: Boolean = false,
    val showLeaderboard: Boolean = true,
    val leaderboard: LeaderboardSettings = LeaderboardSettings(),
)

data class LeaderboardSettings(
    val showScores: Boolean = true,
    val limit: Int = 10,
)

data class SurveyPage(
    val title: String? = null,
    val description: String? = null,
    val imagePath: String? = null,

    val content: List<SurveyPageContent>,
)

//### Survey question hierarchy

// Survey question top class
sealed class SurveyPageContent(
    val type: SurveyContentType,
    val title: String,
    val id: String,
    val required: Boolean = true,
)

enum class SurveyContentType {
    TEXT,
    CHOICE,
    NAME,
    RATING,
    LIKERT,
}

// Text question
class TextQuestion(
    title: String,
    id: String,
    required: Boolean = true,

    val multiline: Boolean = false,
    val score: Int? = null,
    val correctAnswer: String? = null,
) : SurveyPageContent(SurveyContentType.TEXT, title, id, required)

// Choice question
class ChoiceQuestion(
    title: String,
    id: String,
    required: Boolean = true,

    val multiple: Boolean = false,
    val limit: Int = 2,
    val choices: List<ChoiceItem>,
) : SurveyPageContent(SurveyContentType.CHOICE, title, id, required)

data class ChoiceItem(
    val title: String,
    val score: Int? = null,
    val correct: Boolean = false,
)

// Name question
class NameQuestion(
    title: String,
    id: String,
    required: Boolean = true,
) : SurveyPageContent(SurveyContentType.NAME, title, id, required)

// Rating question
class RatingQuestion(
    title: String,
    id: String,
    required: Boolean = true,
) : SurveyPageContent(SurveyContentType.RATING, title, id, required)

// Likert question
class LikertQuestion(
    title: String,
    id: String,
    required: Boolean = true,

    val choices: List<String>,
    val statements: List<LikertStatement>,
) : SurveyPageContent(SurveyContentType.LIKERT, title, id, required)

data class LikertStatement(
    val title: String,
    val score: Int? = null,
    val correctChoice: String? = null,
)
