package com.zinkel.survey.data

import com.zinkel.survey.config.ChoiceQuestion
import com.zinkel.survey.config.DataQuestion
import com.zinkel.survey.config.DataQuestionType
import com.zinkel.survey.config.InformationBlock
import com.zinkel.survey.config.LikertQuestion
import com.zinkel.survey.config.RatingQuestion
import com.zinkel.survey.config.SurveyPageContent
import com.zinkel.survey.config.TextQuestion
import org.jetbrains.compose.resources.StringResource
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.validation_error_pattern_custom
import surveytool.composeapp.generated.resources.validation_error_pattern_email
import surveytool.composeapp.generated.resources.validation_error_pattern_name
import surveytool.composeapp.generated.resources.validation_error_pattern_phone
import surveytool.composeapp.generated.resources.validation_error_required

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
        else AnswerValidationResult(true)

    override fun calculateScore(): Int =
        if (isAnswered() && question.correctAnswer == answer) question.score ?: 0
        else 0
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
        question.required && answer.isNullOrBlank()  -> AnswerValidationResult(false, listOf(Res.string.validation_error_required))
        !question.required && answer.isNullOrBlank() -> AnswerValidationResult(true)
        question.dataType == DataQuestionType.NAME   -> checkPattern(answer, validationRegex ?: NAME_PATTERN, Res.string.validation_error_pattern_name)
        question.dataType == DataQuestionType.PHONE  -> checkPattern(answer, validationRegex ?: PHONE_PATTERN, Res.string.validation_error_pattern_phone)
        question.dataType == DataQuestionType.EMAIL  -> checkPattern(answer, validationRegex ?: EMAIL_PATTERN, Res.string.validation_error_pattern_email)
        question.dataType == DataQuestionType.CUSTOM -> checkPattern(answer, validationRegex, Res.string.validation_error_pattern_custom)
        else                                         -> AnswerValidationResult(true)
    }

    private fun checkPattern(answer: String?, validationPattern: Regex?, error: StringResource) = when {
        answer.isNullOrBlank() || validationPattern == null -> AnswerValidationResult(true)
        validationPattern.matches(answer)                   -> AnswerValidationResult(true)
        else                                                -> AnswerValidationResult(false, listOf(error))
    }

    override fun calculateScore(): Int = 0

    companion object {
        private val NAME_PATTERN = "^[a-zA-Z\\s]{3,40}$".toRegex()
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
