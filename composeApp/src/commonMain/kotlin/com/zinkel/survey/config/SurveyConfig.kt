package com.zinkel.survey.config

import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * Root configuration model describing a single survey or quiz.
 *
 * This immutable data class is typically created by parsing a configuration source (e.g. via YAML)
 * and contains all information required to render the survey UI.
 *
 * Semantics and expectations:
 * - The list of [pages] defines the end-to-end flow; its order is the navigation order.
 * - When [type] is a quiz, the [score] settings control scoring behavior and leaderboard visibility.
 * - [image] & [backgroundImage], when present, can be used by the UI for illustration.
 *
 * Invariants:
 * - [title] should be non-blank for a good UX.
 * - [pages] should contain at least one page to produce a meaningful survey.
 *
 * Thread-safety:
 * - As an immutable value object, instances are safe to share across threads.
 *
 * @property title Human-readable title displayed prominently in the UI.
 * @property description Optional longer description shown on the summary/intro screen.
 * @property type The mode of the survey (SURVEY vs QUIZ), influencing scoring and UI hints.
 * @property image Optional file to an image part of description; interpretation is up to the hosting UI.
 * @property backgroundImage Optional file to a background image; interpretation is up to the hosting UI.
 * @property score Scoring and leaderboard configuration; relevant when [type] implies scoring (e.g., QUIZ).
 * @property pages Ordered list of pages forming the survey/quiz; each page holds its own content items.
 */
data class SurveyConfig(
    val title: String,
    val description: String,
    val type: SurveyType = SurveyType.SURVEY,
    val image: File? = null,
    val backgroundImage: File? = null,

    val score: ScoreSettings = ScoreSettings(),

    val pages: List<SurveyPage>,
)

enum class SurveyType {
    SURVEY,
    QUIZ
}

/**
 * Global scoring options for a survey/quiz.
 *
 * These settings influence how scores are presented in the UI.
 *
 * @property showQuestionScores Whether per-question scores should be shown to the participant.
 * @property showLeaderboard Whether a leaderboard should be shown on the summary screen.
 * @property leaderboard Settings specific to the leaderboard (visibility, limits, etc.).
 */
data class ScoreSettings(
    val showQuestionScores: Boolean = false,
    val showLeaderboard: Boolean = true,
    val leaderboard: LeaderboardSettings = LeaderboardSettings(),
)

/**
 * Leaderboard configuration details.
 *
 * @property showScores Whether numeric scores are displayed next to participant names.
 * @property showPlaceholder Whether the leaderboard should be filled with placeholder lines if not enough quiz runs are available
 * @property limit Maximum number of entries to display in the leaderboard.
 * @property backgroundImage Optional file to a background image.
 */
data class LeaderboardSettings(
    val showScores: Boolean = true,
    val showPlaceholder: Boolean = true,
    val limit: Int = 10,
    val backgroundImage: File? = null,
)

/**
 * A single page in the survey.
 *
 * Pages are presented in the order they appear in [SurveyConfig.pages].
 * Each page contains multiple [SurveyPageContent] items (questions/blocks).
 *
 * @property title Optional page title.
 * @property description Optional page description to provide context.
 * @property image Optional image file to illustrate the page.
 * @property content List of content items (questions) shown on this page.
 */
data class SurveyPage(
    val title: String? = null,
    val description: String? = null,
    val image: File? = null,
    val conditional: ConditionalSettings? = null,

    val content: List<SurveyPageContent>,
)

/**
 * Setting for conditional display.
 *
 * Elements & Pages can be hidden/shown based on the users selection of other elements.
 *
 * @property key Reference to a conditional key. This must be configured and match with a choice question.
 * @property values List of valid values for the referenced key to show this element.
 */
data class ConditionalSettings(
    val key: String,
    val values: List<String>,
)

//### Survey question hierarchy
/**
 * Base class for all survey page content items (questions/blocks).
 *
 * @property type Specific content type used by renderers and validators.
 * @property title Human-readable label or question prompt.
 * @property id Stable identifier used to correlate answers and validations.
 * @property required Whether the question needs an answer.
 * @property savable Whether this content should be saved.
 */
sealed class SurveyPageContent(
    val type: SurveyContentType,
    val title: String,
    val id: String,
    val required: Boolean = true,
    val savable: Boolean = true,
    val conditional: ConditionalSettings? = null,
)

enum class SurveyContentType {
    TEXT,
    CHOICE,
    DATA,
    RATING,
    LIKERT,
    INFORMATION,
    DATETIME,
    SLIDER,
}

/**
 * Free-text question.
 *
 * @param multiline Whether the text field should allow multiple lines.
 * @param pattern Optional regular expression to validate the input.
 * @param score Optional numeric score to award for a correct answer (relevant in quizzes).
 * @param correctAnswer Optional correct answer for scoring/validation in quizzes.
 * @param correctAnswerPattern Optional regular expression to validate the correct answer.
 * @param correctAnswerList Optional list of correct answers for scoring/validation in quizzes.
 */
class TextQuestion(
    title: String,
    id: String,
    required: Boolean = true,
    conditional: ConditionalSettings? = null,

    val multiline: Boolean = false,
    val pattern: Regex? = null,
    val score: Int? = null,
    val correctAnswer: String? = null,
    val correctAnswerPattern: Regex? = null,
    val correctAnswerList: List<String>? = null,
) : SurveyPageContent(SurveyContentType.TEXT, title = title, id = id, required = required, conditional = conditional)

/**
 * Choice-based question.
 *
 * Supports single or multiple selection among a list of [choices]. For multiple selection,
 * [limit] can be used to constrain the number of selections.
 *
 * @param multiple Whether multiple selections are allowed.
 * @param limit Maximum number of selections allowed when [multiple] is true.
 * @param choices The available options for the question.
 */
class ChoiceQuestion(
    title: String,
    id: String,
    required: Boolean = true,
    conditional: ConditionalSettings? = null,

    val multiple: Boolean = false,
    val limit: Int = 2,
    val dropdown: Boolean = false,
    val horizontal: Boolean = false,
    val conditionalKey: String? = null,
    val choices: List<ChoiceItem>,
) : SurveyPageContent(SurveyContentType.CHOICE, title = title, id = id, required = required, conditional = conditional)

/**
 * A single option in a [ChoiceQuestion].
 *
 * @property title Display text for the option.
 * @property score Optional score associated with choosing this option (quizzes).
 * @property correct Whether this option is considered correct (quizzes).
 */
data class ChoiceItem(
    val title: String,
    val score: Int? = null,
    val correct: Boolean = false,
)

/**
 * Question for capturing participant’s details.
 *
 * Typically used to label results or leaderboard entries.
 *
 * @property dataType The type of data to capture (e.g., "Name", "Email", "Phone").
 * @property validationPattern Optional regular expression to validate the input. If null, defaults for each dataType except CUSTOM are used.
 * @property useForLeaderboard Whether this data should be used as highscore name.
 */
class DataQuestion(
    title: String,
    id: String,
    required: Boolean = true,
    conditional: ConditionalSettings? = null,

    val dataType: DataQuestionType = DataQuestionType.NAME,
    val validationPattern: String? = null,
    val useForLeaderboard: Boolean = true,
) : SurveyPageContent(SurveyContentType.DATA, title = title, id = id, required = required, conditional = conditional)

enum class DataQuestionType {
    NAME,
    EMAIL,
    PHONE,
    CUSTOM,
    NICKNAME,
    AGE,
    BIRTHDAY,
}

/**
 * DateTime question.
 *
 * Question a date and/or time input from participant.
 *
 * @property inputType The type of input to show (date, time, date & time).
 * @property initialSelectedTime Optional initial time value to pre-select.
 * @property initialSelectedDate Optional initial date value to pre-select.
 * @property score Optional numeric score to award for a correct answer (relevant in quizzes).
 * @property correctTimeAnswer Optional correct time answer for scoring/validation in quizzes.
 * @property correctDateAnswer Optional correct date answer for scoring/validation in quizzes.
 */
class DateTimeQuestion(
    title: String,
    id: String,
    required: Boolean = true,
    conditional: ConditionalSettings? = null,

    val inputType: DateTimeType = DateTimeType.DATETIME,
    val initialSelectedTime: LocalTime? = null,
    val initialSelectedDate: LocalDate? = null,

    val score: Int? = null,
    val correctTimeAnswer: LocalTime? = null,
    val correctDateAnswer: LocalDate? = null,
) : SurveyPageContent(SurveyContentType.DATETIME, title = title, id = id, required = required, conditional = conditional)

enum class DateTimeType {
    DATE,
    TIME,
    DATETIME,
}

/**
 * Numeric rating question.
 *
 * Presents a discrete rating scale where participants select one value from a fixed set of options.
 * Common use cases include satisfaction ratings, quality assessments, or preference indicators.
 *
 * The rating is displayed as clickable symbols (stars, hearts, etc.) that participants can select.
 * Unlike slider-based questions, this provides a different user experience.
 *
 * @param level The number of rating levels/steps (e.g., 5 for a 1-5 star rating). Must be positive.
 * @param symbol The visual symbol used to represent each rating level (star, heart, like, smile, or number).
 * @param colorGradient Optional color gradient applied across the rating scale (e.g., red to green).
 */
class RatingQuestion(
    title: String,
    id: String,
    required: Boolean = true,
    conditional: ConditionalSettings? = null,

    val level: Int = 5,
    val symbol: RatingSymbol = RatingSymbol.STAR,
    val colorGradient: RatingColorGradient = RatingColorGradient.NONE,
) : SurveyPageContent(SurveyContentType.RATING, title = title, id = id, required = required, conditional = conditional)

enum class RatingSymbol {
    STAR,
    HEART,
    LIKE,
    SMILE,
    NUMBER,
}

enum class RatingColorGradient {
    NONE,
    RED2GREEN
}

/**
 * Slider question.
 *
 * Presents a slider control where participants select a numeric value or range within defined bounds.
 * Useful for capturing continuous or stepped numeric input like temperature, age, or satisfaction levels.
 *
 * @param range Whether the slider allows selecting a range (start and end values) instead of a single value.
 * @param start The minimum value of the slider range.
 * @param end The maximum value of the slider range.
 * @param steps The steps between start and end values. If 0, allows continuous selection.
 * @param showDecimals Whether to display decimal places on the slider.
 * @param unit Optional unit label to display with the value (e.g., "cm", "°C", "%").
 * @param score Optional numeric score to award for a correct answer (relevant in quizzes).
 * @param correctAnswer Optional correct answer for scoring/validation in quizzes.
 */
class SliderQuestion(
    title: String,
    id: String,
    required: Boolean = true,
    conditional: ConditionalSettings? = null,

    val range: Boolean = false,
    val start: Float = 0f,
    val end: Float = 1f,
    val steps: Int = 0,
    val showDecimals: Boolean = false,
    val unit: String? = null,
    val score: Int? = null,
    val correctAnswer: Float? = null,
) : SurveyPageContent(SurveyContentType.SLIDER, title = title, id = id, required = required, conditional = conditional)

/**
 * Likert scale question.
 *
 * Presents a set of [statements] where each is answered by selecting one of the provided
 * [choices] (e.g., "Strongly disagree" .. "Strongly agree").
 *
 * @param choices The ordered Likert choices available for each statement.
 * @param statements The list of statements to be rated/selections to be made.
 */
class LikertQuestion(
    title: String,
    id: String,
    required: Boolean = true,
    conditional: ConditionalSettings? = null,

    val choices: List<String>,
    val statements: List<LikertStatement>,
) : SurveyPageContent(SurveyContentType.LIKERT, title = title, id = id, required = required, conditional = conditional)

/**
 * A single statement within a [LikertQuestion].
 *
 * @property title The statement text.
 * @property score Optional score associated with correctly choosing [correctChoice] (for quizzes).
 * @property correctChoice Optional correct choice value for scoring purposes.
 */
data class LikertStatement(
    val title: String,
    val score: Int? = null,
    val correctChoice: String? = null,
)

/**
 * Information block content. Not a question.
 *
 * Presents information to the user.
 *
 * @param description Information text (besides [title]) to show.
 * @param image Image to show.
 */
class InformationBlock(
    title: String,
    id: String,
    conditional: ConditionalSettings? = null,

    val description: String? = null,
    val image: File? = null,
) : SurveyPageContent(SurveyContentType.INFORMATION, title = title, id = id, required = true, savable = false, conditional = conditional)
