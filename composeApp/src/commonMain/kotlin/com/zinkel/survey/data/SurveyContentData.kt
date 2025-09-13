package com.zinkel.survey.data

import com.zinkel.survey.config.ChoiceQuestion
import com.zinkel.survey.config.LikertQuestion
import com.zinkel.survey.config.NameQuestion
import com.zinkel.survey.config.RatingQuestion
import com.zinkel.survey.config.SurveyPageContent
import com.zinkel.survey.config.TextQuestion
import org.jetbrains.compose.resources.StringResource
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.validation_error_required

sealed class SurveyContentData(
    open val question: SurveyPageContent,
    open val answer: Any? = null,
) {
    abstract fun isAnswered(): Boolean
    abstract fun validate(): AnswerValidationResult
    abstract fun calculateScore(): Int

    companion object {
        fun fromSurveyPageContent(content: SurveyPageContent, answer: Any? = null): SurveyContentData = when (content) {
            is NameQuestion   -> NameSurveyContentData(content, answer as? String)
            is ChoiceQuestion -> ChoiceSurveyContentData(content, answer as? List<String>)
            is LikertQuestion -> LikertSurveyContentData(content, answer as? MutableMap<String, String>)
            is RatingQuestion -> RatingSurveyContentData(content, answer as? Int)
            is TextQuestion   -> TextSurveyContentData(content, answer as? String)
        }
    }
}

data class AnswerValidationResult(
    val isValid: Boolean,
    val validationErrors: List<StringResource>? = null,
)

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

class NameSurveyContentData(
    override val question: NameQuestion,
    override var answer: String? = null,
) : SurveyContentData(question) {

    override fun isAnswered() = !answer.isNullOrBlank()

    override fun validate() =
        if (question.required && answer.isNullOrBlank()) AnswerValidationResult(
            false,
            listOf(Res.string.validation_error_required)
        )
        else AnswerValidationResult(true)

    override fun calculateScore(): Int = 0
}

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
