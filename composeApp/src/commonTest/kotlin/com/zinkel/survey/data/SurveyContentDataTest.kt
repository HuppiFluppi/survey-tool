package com.zinkel.survey.data

import com.zinkel.survey.config.*
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.validation_error_required
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.test.*

class TextSurveyContentDataTest {
    @Test
    fun `isAnswered returns false for null answer`() {
        val question = TextQuestion("Q1", "q1")
        val data = TextSurveyContentData(question, null)
        assertFalse(data.isAnswered())
    }

    @Test
    fun `isAnswered returns true for non-blank answer`() {
        val question = TextQuestion("Q1", "q1")
        val data = TextSurveyContentData(question, "answer")
        assertTrue(data.isAnswered())
    }

    @Test
    fun `validate fails when required and blank`() {
        val question = TextQuestion("Q1", "q1", required = true)
        val data = TextSurveyContentData(question, "")
        val result = data.validate()
        assertFalse(result.isValid)
        assertEquals(Res.string.validation_error_required, result.validationErrors?.first())
    }

    @Test
    fun `validate succeeds when not required and blank`() {
        val question = TextQuestion("Q1", "q1", required = false)
        val data = TextSurveyContentData(question, "")
        assertTrue(data.validate().isValid)
    }

    @Test
    fun `validate fails when pattern does not match`() {
        val question = TextQuestion("Q1", "q1", pattern = "\\d+".toRegex())
        val data = TextSurveyContentData(question, "abc")
        assertFalse(data.validate().isValid)
    }

    @Test
    fun `calculateScore returns score for correct answer`() {
        val question = TextQuestion("Q1", "q1", score = 10, correctAnswer = "correct")
        val data = TextSurveyContentData(question, "correct")
        assertEquals(10, data.calculateScore())
    }

    @Test
    fun `calculateScore returns zero for incorrect answer`() {
        val question = TextQuestion("Q1", "q1", score = 10, correctAnswer = "correct")
        val data = TextSurveyContentData(question, "wrong")
        assertEquals(0, data.calculateScore())
    }

    @Test
    fun `calculateScore returns score for pattern match`() {
        val question = TextQuestion("Q1", "q1", score = 5, correctAnswerPattern = "\\d{3}".toRegex())
        val data = TextSurveyContentData(question, "123")
        assertEquals(5, data.calculateScore())
    }

    @Test
    fun `calculateScore returns score for answer in list`() {
        val question = TextQuestion("Q1", "q1", score = 7, correctAnswerList = listOf("a", "b", "c"))
        val data = TextSurveyContentData(question, "b")
        assertEquals(7, data.calculateScore())
    }
}

class DataSurveyContentDataTest {
    @Test
    fun `validate email pattern`() {
        val question = DataQuestion("Email", "email", dataType = DataQuestionType.EMAIL)
        val data = DataSurveyContentData(question, "test@example.com")
        assertTrue(data.validate().isValid)
    }

    @Test
    fun `validate invalid email`() {
        val question = DataQuestion("Email", "email", dataType = DataQuestionType.EMAIL)
        val data = DataSurveyContentData(question, "invalid-email")
        assertFalse(data.validate().isValid)
    }

    @Test
    fun `validate phone pattern`() {
        val question = DataQuestion("Phone", "phone", dataType = DataQuestionType.PHONE)
        val data = DataSurveyContentData(question, "+1234567890")
        assertTrue(data.validate().isValid)

        data.answer = "+49 172 09715740"
        assertTrue(data.validate().isValid)

        data.answer = "017209715740"
        assertTrue(data.validate().isValid)

        data.answer = "+1 (172) 09715740"
        assertTrue(data.validate().isValid)
    }

    @Test
    fun `validate name pattern`() {
        val question = DataQuestion("Name", "name", dataType = DataQuestionType.NAME)
        val data = DataSurveyContentData(question, "John Doe")
        assertTrue(data.validate().isValid)

        data.answer = "Jürgen Schonlì"
        assertTrue(data.validate().isValid)

        data.answer = "Josè Nuño"
        assertTrue(data.validate().isValid)
    }

    @Test
    fun `validate age in range`() {
        val question = DataQuestion("Age", "age", dataType = DataQuestionType.AGE)
        val data = DataSurveyContentData(question, "25")
        assertTrue(data.validate().isValid)
    }

    @Test
    fun `validate age out of range`() {
        val question = DataQuestion("Age", "age", dataType = DataQuestionType.AGE)
        val data = DataSurveyContentData(question, "150")
        assertFalse(data.validate().isValid)
    }

    @Test
    fun `calculateScore always returns zero`() {
        val question = DataQuestion("Name", "name")
        val data = DataSurveyContentData(question, "John")
        assertEquals(0, data.calculateScore())
    }
}

class RatingSurveyContentDataTest {
    @Test
    fun `isAnswered returns true when answer is set`() {
        val question = RatingQuestion("Rate", "rate")
        val data = RatingSurveyContentData(question, 5)
        assertTrue(data.isAnswered())
    }

    @Test
    fun `isAnswered returns false when answer is null`() {
        val question = RatingQuestion("Rate", "rate")
        val data = RatingSurveyContentData(question, null)
        assertFalse(data.isAnswered())
    }

    @Test
    fun `validate fails when required and null`() {
        val question = RatingQuestion("Rate", "rate", required = true)
        val data = RatingSurveyContentData(question, null)
        assertFalse(data.validate().isValid)
    }

    @Test
    fun `calculateScore returns zero`() {
        val question = RatingQuestion("Rate", "rate")
        val data = RatingSurveyContentData(question, 3)
        assertEquals(0, data.calculateScore())
    }
}

class SliderSurveyContentDataTest {
    @Test
    fun `isAnswered returns true when answer is set`() {
        val question = SliderQuestion("Slider", "slider")
        val data = SliderSurveyContentData(question, Pair(5f, null))
        assertTrue(data.isAnswered())
    }

    @Test
    fun `validate fails when required and null`() {
        val question = SliderQuestion("Slider", "slider", required = true)
        val data = SliderSurveyContentData(question, null)
        assertFalse(data.validate().isValid)
    }

    @Test
    fun `calculateScore returns score for exact match`() {
        val question = SliderQuestion("Slider", "slider", score = 10, correctAnswer = 5f)
        val data = SliderSurveyContentData(question, Pair(5f, null))
        assertEquals(10, data.calculateScore())
    }

    @Test
    fun `calculateScore returns score for range match`() {
        val question = SliderQuestion("Slider", "slider", range = true, score = 10, correctAnswer = 5f)
        val data = SliderSurveyContentData(question, Pair(3f, 7f))
        assertEquals(10, data.calculateScore())
    }

    @Test
    fun `calculateScore returns zero for incorrect answer`() {
        val question = SliderQuestion("Slider", "slider", score = 10, correctAnswer = 5f)
        val data = SliderSurveyContentData(question, Pair(3f, null))
        assertEquals(0, data.calculateScore())
    }
}

class ChoiceSurveyContentDataTest {
    @Test
    fun `isAnswered returns false for empty list`() {
        val question = ChoiceQuestion("Choice", "choice", choices = listOf())
        val data = ChoiceSurveyContentData(question, emptyList())
        assertFalse(data.isAnswered())
    }

    @Test
    fun `isAnswered returns true for non-empty list`() {
        val question = ChoiceQuestion("Choice", "choice", choices = listOf())
        val data = ChoiceSurveyContentData(question, listOf("A"))
        assertTrue(data.isAnswered())
    }

    @Test
    fun `validate fails when required and empty`() {
        val question = ChoiceQuestion("Choice", "choice", required = true, choices = listOf())
        val data = ChoiceSurveyContentData(question, emptyList())
        assertFalse(data.validate().isValid)
    }

    @Test
    fun `calculateScore sums correct choices`() {
        val choices = listOf(
            ChoiceItem("A", score = 5, correct = true),
            ChoiceItem("B", score = 3, correct = true),
            ChoiceItem("C", score = 2, correct = false)
        )
        val question = ChoiceQuestion("Choice", "choice", multiple = true, choices = choices)
        val data = ChoiceSurveyContentData(question, listOf("A", "B"))
        assertEquals(8, data.calculateScore())
    }

    @Test
    fun `calculateScore returns zero for incorrect choices`() {
        val choices = listOf(
            ChoiceItem("A", score = 5, correct = true),
            ChoiceItem("B", score = 3, correct = false)
        )
        val question = ChoiceQuestion("Choice", "choice", choices = choices)
        val data = ChoiceSurveyContentData(question, listOf("B"))
        assertEquals(0, data.calculateScore())
    }
}

class LikertSurveyContentDataTest {
    @Test
    fun `isAnswered returns false for empty map`() {
        val question = LikertQuestion("Likert", "likert", choices = listOf(), statements = listOf())
        val data = LikertSurveyContentData(question, mutableMapOf())
        assertFalse(data.isAnswered())
    }

    @Test
    fun `validate fails when required and incomplete`() {
        val statements = listOf(LikertStatement("S1"), LikertStatement("S2"))
        val question = LikertQuestion("Likert", "likert", required = true, choices = listOf(), statements = statements)
        val data = LikertSurveyContentData(question, mutableMapOf("S1" to "Agree"))
        assertFalse(data.validate().isValid)
    }

    @Test
    fun `validate succeeds when all statements answered`() {
        val statements = listOf(LikertStatement("S1"), LikertStatement("S2"))
        val question = LikertQuestion("Likert", "likert", required = true, choices = listOf(), statements = statements)
        val data = LikertSurveyContentData(question, mutableMapOf("S1" to "Agree", "S2" to "Disagree"))
        assertTrue(data.validate().isValid)
    }

    @Test
    fun `calculateScore sums correct answers`() {
        val statements = listOf(
            LikertStatement("S1", score = 5, correctChoice = "Agree"),
            LikertStatement("S2", score = 3, correctChoice = "Disagree"),
            LikertStatement("S3", score = 1, correctChoice = "Disagree"),
        )
        val question = LikertQuestion("Likert", "likert", choices = listOf(), statements = statements)
        val data = LikertSurveyContentData(question, mutableMapOf("S1" to "Agree", "S2" to "Disagree", "S3" to "Agree"))
        assertEquals(8, data.calculateScore())
    }
}

class InformationSurveyContentDataTest {
    @Test
    fun `isAnswered always returns true`() {
        val question = InformationBlock("Info", "info")
        val data = InformationSurveyContentData(question)
        assertTrue(data.isAnswered())
    }

    @Test
    fun `validate always succeeds`() {
        val question = InformationBlock("Info", "info")
        val data = InformationSurveyContentData(question)
        assertTrue(data.validate().isValid)
    }

    @Test
    fun `calculateScore returns zero`() {
        val question = InformationBlock("Info", "info")
        val data = InformationSurveyContentData(question)
        assertEquals(0, data.calculateScore())
    }
}

class DateTimeSurveyContentDataTest {
    @Test
    fun `isAnswered returns true for date and time`() {
        val question = DateTimeQuestion("DateTime", "dt", inputType = DateTimeType.DATETIME)
        val data = DateTimeSurveyContentData(question, DateTimePick(LocalDate.now(), LocalTime.now()))
        assertTrue(data.isAnswered())
    }

    @Test
    fun `isAnswered returns true for date only when type is DATE`() {
        val question = DateTimeQuestion("Date", "d", inputType = DateTimeType.DATE)
        val data = DateTimeSurveyContentData(question, DateTimePick(LocalDate.now(), null))
        assertTrue(data.isAnswered())
    }

    @Test
    fun `isAnswered returns true for time only when type is TIME`() {
        val question = DateTimeQuestion("Time", "t", inputType = DateTimeType.TIME)
        val data = DateTimeSurveyContentData(question, DateTimePick(null, LocalTime.now()))
        assertTrue(data.isAnswered())
    }

    @Test
    fun `validate fails when required and not answered`() {
        val question = DateTimeQuestion("DateTime", "dt", required = true)
        val data = DateTimeSurveyContentData(question, null)
        assertFalse(data.validate().isValid)
    }

    @Test
    fun `calculateScore returns score for correct date and time`() {
        val date = LocalDate.of(2024, 1, 1)
        val time = LocalTime.of(12, 0)
        val question = DateTimeQuestion("DateTime", "dt", score = 10, correctDateAnswer = date, correctTimeAnswer = time)
        val data = DateTimeSurveyContentData(question, DateTimePick(date, time))
        assertEquals(10, data.calculateScore())
    }

    @Test
    fun `calculateScore returns zero for incorrect answer`() {
        val date = LocalDate.of(2024, 1, 1)
        val time = LocalTime.of(12, 0)
        val question = DateTimeQuestion("DateTime", "dt", score = 10, correctDateAnswer = date, correctTimeAnswer = time)
        val data = DateTimeSurveyContentData(question, DateTimePick(date.plus(1, ChronoUnit.DAYS), time))
        assertEquals(0, data.calculateScore())
    }

    @Test
    fun `init prefills answer with initial values`() {
        val date = LocalDate.of(2024, 1, 1)
        val time = LocalTime.of(12, 0)
        val question = DateTimeQuestion("DateTime", "dt", initialSelectedDate = date, initialSelectedTime = time)
        val data = DateTimeSurveyContentData(question, null)
        assertEquals(date, data.answer?.date)
        assertEquals(time, data.answer?.time)
    }
}

class DateTimePickTest {
    @Test
    fun `toString returns empty for null date and time`() {
        val pick = DateTimePick(null, null)
        assertEquals("", pick.toString())
    }

    @Test
    fun `toString returns date only`() {
        val date = LocalDate.of(2024, 1, 1)
        val pick = DateTimePick(date, null)
        assertEquals("2024-01-01", pick.toString())
    }

    @Test
    fun `toString returns time only`() {
        val time = LocalTime.of(12, 30)
        val pick = DateTimePick(null, time)
        assertEquals("12:30", pick.toString())
    }

    @Test
    fun `toString returns date and time`() {
        val date = LocalDate.of(2024, 1, 1)
        val time = LocalTime.of(12, 30)
        val pick = DateTimePick(date, time)
        assertEquals("2024-01-01 12:30", pick.toString())
    }
}

class SurveyContentDataFactoryTest {
    @Test
    fun `fromSurveyPageContent creates TextSurveyContentData`() {
        val question = TextQuestion("Q", "q")
        val data = SurveyContentData.fromSurveyPageContent(question, "answer")
        assertTrue(data is TextSurveyContentData)
        assertEquals("answer", data.answer)
    }

    @Test
    fun `fromSurveyPageContent creates ChoiceSurveyContentData`() {
        val question = ChoiceQuestion("Q", "q", choices = listOf())
        val data = SurveyContentData.fromSurveyPageContent(question, listOf("A"))
        assertTrue(data is ChoiceSurveyContentData)
        assertEquals(listOf("A"), data.answer)
    }

    @Test
    fun `fromSurveyPageContent creates RatingSurveyContentData`() {
        val question = RatingQuestion("Q", "q")
        val data = SurveyContentData.fromSurveyPageContent(question, 5)
        assertTrue(data is RatingSurveyContentData)
        assertEquals(5, data.answer)
    }

    @Test
    fun `fromSurveyPageContent creates DataSurveyContentData`() {
        val question = DataQuestion("Q", "q")
        val data = SurveyContentData.fromSurveyPageContent(question, "John")
        assertTrue(data is DataSurveyContentData)
        assertEquals("John", data.answer)
    }

    @Test
    fun `fromSurveyPageContent creates LikertSurveyContentData`() {
        val question = LikertQuestion("Q", "q", choices = listOf(), statements = listOf())
        val answer = mutableMapOf("S1" to "Agree")
        val data = SurveyContentData.fromSurveyPageContent(question, answer)
        assertTrue(data is LikertSurveyContentData)
        assertEquals(answer, data.answer)
    }

    @Test
    fun `fromSurveyPageContent creates InformationSurveyContentData`() {
        val question = InformationBlock("Info", "info")
        val data = SurveyContentData.fromSurveyPageContent(question)
        assertTrue(data is InformationSurveyContentData)
    }

    @Test
    fun `fromSurveyPageContent creates DateTimeSurveyContentData`() {
        val question = DateTimeQuestion("Q", "q")
        val pick = DateTimePick(LocalDate.now(), LocalTime.now())
        val data = SurveyContentData.fromSurveyPageContent(question, pick)
        assertTrue(data is DateTimeSurveyContentData)
        assertEquals(pick, data.answer)
    }

    @Test
    fun `fromSurveyPageContent creates SliderSurveyContentData`() {
        val question = SliderQuestion("Q", "q")
        val answer = Pair(5f, null)
        val data = SurveyContentData.fromSurveyPageContent(question, answer)
        assertTrue(data is SliderSurveyContentData)
        assertEquals(answer, data.answer)
    }
}
