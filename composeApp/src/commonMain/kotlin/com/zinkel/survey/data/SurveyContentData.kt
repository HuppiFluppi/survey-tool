package com.zinkel.survey.data

import com.zinkel.survey.config.*
import org.jetbrains.compose.resources.StringResource
import surveytool.composeapp.generated.resources.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import kotlin.math.abs

/**
 * Runtime model for a single question (content item) on a survey page.
 *
 * Wraps the static [question] definition and the mutable [answer]. Subclasses constrain
 * the type of [answer] and implement validation and scoring rules accordingly.
 *
 * Contract:
 * - [isAnswered] reports whether an answer is present (type-specific).
 * - [validate] returns an [AnswerValidationResult] suitable for showing messages to the user.
 * - [calculateScore] computes per-question score (for quizzes); default is subclass-specific.
 */
sealed class SurveyContentData(
    open val question: SurveyPageContent,
    open val answer: Any? = null,
) {
    /**
     * Whether the current answer satisfies the notion of "being present".
     * For example, non-blank text, non-empty selection, or non-null numeric value.
     */
    abstract fun isAnswered(): Boolean

    /**
     * Validates the current answer against question constraints (e.g., required).
     * Implementations return error keys that can be rendered by the UI.
     */
    abstract fun validate(): AnswerValidationResult

    /**
     * Computes the score contributed by this question.
     * Non-quiz content usually returns 0; quiz content may compute based on correctness.
     */
    abstract fun calculateScore(): Int

    companion object {
        /**
         * Creates a concrete [SurveyContentData] instance for the given [content],
         * seeding it with an optional [answer].
         *
         * The [answer] must match the expected type of the question, otherwise it is ignored.
         */
        fun fromSurveyPageContent(content: SurveyPageContent, answer: Any? = null): SurveyContentData = when (content) {
            is DataQuestion     -> DataSurveyContentData(content, answer as? String)
            is ChoiceQuestion   -> ChoiceSurveyContentData(content, answer as? List<String>)
            is LikertQuestion   -> LikertSurveyContentData(content, answer as? MutableMap<String, String>)
            is RatingQuestion   -> RatingSurveyContentData(content, answer as? Int)
            is TextQuestion     -> TextSurveyContentData(content, answer as? String)
            is InformationBlock -> InformationSurveyContentData(content)
            is DateTimeQuestion -> DateTimeSurveyContentData(content, answer as? DateTimePick)
            is SliderQuestion   -> SliderSurveyContentData(content, answer as? Pair<Float, Float?>)
        }
    }
}

/**
 * Result of validating a user's answer.
 *
 * @property isValid True if the answer is acceptable and the user can proceed.
 * @property validationErrors Optional list of string resource keys describing validation issues.
 */
data class AnswerValidationResult(
    val isValid: Boolean,
    val validationErrors: List<StringResource>? = null,
)

/**
 * Free-text question runtime model.
 *
 * Answer is a nullable [String]. If question is required, blank answers are invalid.
 * Scoring (for quizzes) awards [TextQuestion.score] when [TextQuestion.correctAnswer] matches.
 */
class TextSurveyContentData(
    override val question: TextQuestion,
    override var answer: String? = null,
) : SurveyContentData(question) {

    override fun isAnswered() = !answer.isNullOrBlank()

    override fun validate() =
        if (question.required && answer.isNullOrBlank()) AnswerValidationResult(
            false,
            listOf(Res.string.validation_error_required)
        )
        else if (isAnswered()
            && question.pattern != null
            && answer != null
            && !answer!!.matches(question.pattern)
        ) AnswerValidationResult(false, listOf(Res.string.validation_error_pattern_text))
        else AnswerValidationResult(true)

    override fun calculateScore(): Int = when {
        isAnswered() && question.correctAnswer != null        -> if (question.correctAnswer == answer!!.trim()) question.score ?: 0 else 0
        isAnswered() && question.correctAnswerPattern != null -> if (answer!!.trim().matches(question.correctAnswerPattern)) question.score ?: 0 else 0
        isAnswered() && question.correctAnswerList != null    -> if (answer!!.trim() in question.correctAnswerList) question.score ?: 0 else 0
        else                                                  -> 0
    }
}

/**
 * Data question runtime model.
 *
 * Answer is a nullable [String]. Used for collecting participants details like name (e.g. on leaderboards).
 * Validation enforces required semantics and pattern; scoring is always 0.
 */
class DataSurveyContentData(
    override val question: DataQuestion,
    override var answer: String? = null,
) : SurveyContentData(question) {
    private val validationRegex = question.validationPattern?.toRegex()

    override fun isAnswered() = !answer.isNullOrBlank()

    override fun validate() = when {
        answer.isNullOrBlank() && question.required    -> AnswerValidationResult(false, listOf(Res.string.validation_error_required))
        answer.isNullOrBlank() && !question.required   -> AnswerValidationResult(true)
        question.dataType == DataQuestionType.NAME     -> checkPattern(answer!!, validationRegex ?: NAME_PATTERN, Res.string.validation_error_pattern_name)
        question.dataType == DataQuestionType.PHONE    -> checkPattern(answer!!, validationRegex ?: PHONE_PATTERN, Res.string.validation_error_pattern_phone)
        question.dataType == DataQuestionType.EMAIL    -> checkPattern(answer!!, validationRegex ?: EMAIL_PATTERN, Res.string.validation_error_pattern_email)
        question.dataType == DataQuestionType.CUSTOM   -> checkPattern(answer!!, validationRegex, Res.string.validation_error_pattern_custom)
        question.dataType == DataQuestionType.NICKNAME -> checkPattern(
            answer!!,
            validationRegex ?: NICKNAME_PATTERN,
            Res.string.validation_error_pattern_nickname
        )

        question.dataType == DataQuestionType.AGE      -> checkAge(answer!!)
        question.dataType == DataQuestionType.BIRTHDAY -> checkBirthday(answer!!)
        else                                           -> AnswerValidationResult(true)
    }

    private fun checkBirthday(answer: String): AnswerValidationResult {
        try {
            val dtf = DateTimeFormatterBuilder().parseLenient().appendLocalized(FormatStyle.SHORT, null).toFormatter()
            val birthday = LocalDate.parse(answer, dtf)
            val today = LocalDate.now()
            if ((today.year - birthday.year) !in 0..100) return AnswerValidationResult(false, listOf(Res.string.validation_error_pattern_birthday))
        } catch (e: Exception) {
            e.printStackTrace()
            return AnswerValidationResult(false, listOf(Res.string.validation_error_pattern_birthday))
        }
        return AnswerValidationResult(true)
    }

    private fun checkAge(answer: String): AnswerValidationResult {
        val age = answer.toIntOrNull() ?: return AnswerValidationResult(false, listOf(Res.string.validation_error_pattern_age))
        return when {
            age in 1..100 -> AnswerValidationResult(true)
            else          -> AnswerValidationResult(false, listOf(Res.string.validation_error_pattern_age))
        }
    }

    private fun checkPattern(answer: String, validationPattern: Regex?, error: StringResource) = when {
        answer.isBlank() || validationPattern == null -> AnswerValidationResult(true)
        validationPattern.matches(answer)             -> AnswerValidationResult(true)
        else                                          -> AnswerValidationResult(false, listOf(error))
    }

    override fun calculateScore(): Int = 0

    companion object {
        private val NAME_PATTERN = "^\\p{L}[\\p{L} ]{2,40}$".toRegex()
        private val NICKNAME_PATTERN = "^[\\p{L} \\d._-]{3,40}$".toRegex()
        private val PHONE_PATTERN = "^((\\+|00)[1-9]{1,2})?[0-9 \\-()./]{6,32}$".toRegex()
        private val EMAIL_PATTERN = "^[a-zA-Z0-9+._%-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9-]{0,64}(.[a-zA-Z0-9][a-zA-Z0-9-]{0,25})+$".toRegex()
    }
}

/**
 * Numeric rating question runtime model.
 *
 * Answer is a nullable [Int]. Validation enforces required semantics; scoring is always 0.
 * The visual scale (e.g., 1..5) is defined by UI or higher-level configuration.
 */
class RatingSurveyContentData(
    override val question: RatingQuestion,
    override var answer: Int? = null,
) : SurveyContentData(question) {

    override fun isAnswered() = answer != null

    override fun validate() =
        if (question.required && (answer == null)) AnswerValidationResult(
            false,
            listOf(Res.string.validation_error_required)
        )
        else AnswerValidationResult(true)

    override fun calculateScore(): Int = 0
}

/**
 * Slider question runtime model.
 *
 * Answer is a nullable [Pair<Float, Float?>], first entry holding begin or only value, second entry holding end for range slider.
 * - Validation enforces required semantics.
 * - Scoring ensures [question.correctAnswer] is same or in the range of [answer].
 */
class SliderSurveyContentData(
    override val question: SliderQuestion,
    override var answer: Pair<Float, Float?>? = null,
) : SurveyContentData(question) {

    override fun isAnswered() = answer != null

    override fun validate() =
        if (question.required && (answer == null)) AnswerValidationResult(
            false,
            listOf(Res.string.validation_error_required)
        )
        else AnswerValidationResult(true)

    override fun calculateScore(): Int {
        if (!isAnswered()) return 0
        if (question.correctAnswer == null) return 0

        // calculation for range
        if (question.range && (question.correctAnswer in (answer!!.first..answer!!.second!!))) return question.score ?: 0

        // calculation for single value
        val div = abs(question.correctAnswer - answer!!.first)
        if (div < 0.000001f) return question.score ?: 0

        return 0
    }
}

/**
 * Choice question runtime model.
 *
 * Answer is a nullable list of selected choice titles.
 * - Validation enforces required semantics (selection presence when required).
 * - Selection limits for multi-select are enforced by UI logic, not re-validated here.
 * - Scoring sums the scores of all chosen choices marked as correct.
 */
class ChoiceSurveyContentData(
    override val question: ChoiceQuestion,
    override var answer: List<String>? = null,
) : SurveyContentData(question) {

    override fun isAnswered() = !answer.isNullOrEmpty()

    override fun validate(): AnswerValidationResult {
        //check required
        if (question.required && answer.isNullOrEmpty()) return AnswerValidationResult(
            false,
            listOf(Res.string.validation_error_required)
        )

        //UI logic is ensuring multiple selection limit is enforced. So we don't check it here
        return AnswerValidationResult(true)
    }

    override fun calculateScore(): Int {
        if (!isAnswered()) return 0

        return answer?.sumOf { answer ->
            val choice = question.choices.find { it.title == answer }
            if (choice?.correct == true) {
                choice.score ?: 0
            } else {
                0
            }
        } ?: 0
    }
}

/**
 * Likert-scale question runtime model.
 *
 * Answer is a nullable map from statement title to chosen choice value.
 * - Validation requires answers for all statements when the question is required.
 * - Scoring sums the scores of statements where the selected choice equals the configured correct choice.
 */
class LikertSurveyContentData(
    override val question: LikertQuestion,
    override var answer: MutableMap<String, String>? = null,
) : SurveyContentData(question) {

    override fun isAnswered() = !answer.isNullOrEmpty()

    override fun validate() =
        if (question.required && answer?.size != question.statements.size) AnswerValidationResult(
            false,
            listOf(Res.string.validation_error_required)
        )
        else AnswerValidationResult(true)

    override fun calculateScore(): Int {
        if (!isAnswered()) return 0

        return question.statements.sumOf { statement ->
            if (statement.correctChoice == answer?.get(statement.title)) statement.score ?: 0
            else 0
        }
    }
}

/**
 * Information content data with dummy answer.
 */
class InformationSurveyContentData(
    override val question: InformationBlock,
    override val answer: Unit = Unit,
) : SurveyContentData(question) {

    override fun isAnswered() = true
    override fun validate() = AnswerValidationResult(true)
    override fun calculateScore(): Int = 0
}

/**
 * Date+Time question runtime model.
 *
 * Answer is a nullable [LocalDateTime]. If question is required, blank answers are invalid.
 * NOTE: It is important to only set answer when the user has selected a complete date+time (if both should be entered).
 * Due to the possibility of giving initial values, the user can accept these without active selection.
 *
 * Scoring (for quizzes) awards [DateTimeQuestion.score] when [DateTimeQuestion.correctTimeAnswer] and [DateTimeQuestion.correctDateAnswer] matches.
 */
class DateTimeSurveyContentData(
    override val question: DateTimeQuestion,
    override var answer: DateTimePick?,
) : SurveyContentData(question) {

    init {
        //prefill answer with given initial values. Hence, allowing the user to just take defaults
        if (question.initialSelectedTime != null || question.initialSelectedDate != null) {
            answer = DateTimePick(question.initialSelectedDate, question.initialSelectedTime)
        }
    }

    override fun isAnswered() = answer != null
            && (answer!!.date != null || question.inputType == DateTimeType.TIME)
            && (answer!!.time != null || question.inputType == DateTimeType.DATE)

    override fun validate() =
        if (question.required && !isAnswered()) AnswerValidationResult(
            false,
            listOf(Res.string.validation_error_required)
        )
        else AnswerValidationResult(true)

    override fun calculateScore(): Int {
        if (!isAnswered()) return 0
        val (date, time) = answer!!
        if (time?.equals(question.correctTimeAnswer) ?: true && date?.equals(question.correctDateAnswer) ?: true) return question.score ?: 0
        return 0
    }
}

data class DateTimePick(
    var date: LocalDate? = null,
    var time: LocalTime? = null,
) {
    override fun toString(): String {
        if (date == null && time == null) return ""
        if (date != null && time == null) return date.toString()
        if (time != null && date == null) return time.toString()
        return "$date $time"
    }
}
