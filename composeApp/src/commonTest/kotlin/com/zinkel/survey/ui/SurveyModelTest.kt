// SurveyModelTest.kt

package com.zinkel.survey.ui

import com.zinkel.survey.config.ChoiceItem
import com.zinkel.survey.config.ChoiceQuestion
import com.zinkel.survey.config.LikertQuestion
import com.zinkel.survey.config.LikertStatement
import com.zinkel.survey.config.DataQuestion
import com.zinkel.survey.config.RatingQuestion
import com.zinkel.survey.config.SurveyConfig
import com.zinkel.survey.config.SurveyPage
import com.zinkel.survey.config.TextQuestion
import com.zinkel.survey.data.LikertSurveyContentData
import com.zinkel.survey.data.TextSurveyContentData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SurveyModelTest {

    @Test
    fun `advanceSurvey checks and reports validation error for unanswered ratingQuestion`() {
        val surveyConfig = SurveyConfig(
            title = "Rating Question Test",
            description = "Test Description",
            pages = listOf(
                SurveyPage(
                    "Page 1",
                    "Page 1 Description",
                    content = listOf(
                        RatingQuestion("RatingQuestion1", "RQ1")
                    )
                )
            )
        )
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val surveyModel = SurveyModel(surveyConfig, File("build/tmp/test"), testScope)

        // Start the survey
        surveyModel.takeSurvey()

        // Attempt to proceed without answering the Rating question
        surveyModel.advanceSurvey()

        // Assert that input validation error is present
        val uiState = surveyModel.surveyUiState
        assertTrue(uiState is SurveyModel.SurveyUiState.Content)
        val inputErrors = uiState.contentUiState.inputErrors
        assertEquals(1, inputErrors.size)
        assertTrue(inputErrors.containsKey("RQ1"))
    }

    @Test
    fun `takeSurvey initializes survey correctly`() {
        val surveyConfig = SurveyConfig(
            title = "Test Survey",
            description = "Test Description",
            pages = listOf(
                SurveyPage(
                    title = "Page 1",
                    description = "Page 1 Description",
                    content = listOf(
                        DataQuestion("Q1", "Question 1"),
                        ChoiceQuestion("Q2", "Question 2", choices = listOf(ChoiceItem("Choice 1"), ChoiceItem("Choice 2")))
                    )
                )
            )
        )

        val surveyModel = SurveyModel(surveyConfig, File("build/tmp/test"), TestScope())

        surveyModel.takeSurvey()

        val uiState = surveyModel.surveyUiState
        assertTrue(uiState is SurveyModel.SurveyUiState.Content)

        val contentState = uiState.contentUiState
        assertEquals("Test Survey", contentState.surveyTitle)
        assertEquals(1, contentState.totalPages)
        assertEquals(1, contentState.currentPage)
        assertEquals("Page 1", contentState.pageTitle)
        assertEquals("Page 1 Description", contentState.pageDescription)
        assertEquals(2, contentState.content.size)
        assertEquals("Question 1", contentState.content[0].question.id)
        assertEquals("Q1", contentState.content[0].question.title)
        assertEquals("Question 2", contentState.content[1].question.id)
        assertEquals("Q2", contentState.content[1].question.title)
    }

    @Test
    fun `cancelSurvey resets survey to summary state`() {
        val surveyConfig = SurveyConfig(
            title = "Test Survey",
            description = "Test Description",
            pages = listOf(SurveyPage("Page 1", "Page 1 Description", content = listOf()))
        )
        val surveyModel = SurveyModel(surveyConfig, File("build/tmp/test"), TestScope())

        surveyModel.takeSurvey()
        assertTrue(surveyModel.surveyUiState is SurveyModel.SurveyUiState.Content)

        surveyModel.cancelSurvey()
        val uiState = surveyModel.surveyUiState
        assertTrue(uiState is SurveyModel.SurveyUiState.Summary)
        assertEquals("Test Survey", uiState.summaryUiState.title)
        assertEquals("Test Description", uiState.summaryUiState.description)
    }

    @Test
    fun `backSurvey moves user to previous page`() {
        val surveyConfig = SurveyConfig(
            title = "Test Survey",
            description = "Test Description",
            pages = listOf(
                SurveyPage("Page 1", "Page 1 Description", content = listOf()),
                SurveyPage("Page 2", "Page 2 Description", content = listOf())
            )
        )
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val surveyModel = SurveyModel(surveyConfig, File("build/tmp/test"), testScope)

        surveyModel.takeSurvey()
        surveyModel.advanceSurvey() // Move to Page 2
        surveyModel.backSurvey() // Move back to Page 1

        val uiState = surveyModel.surveyUiState
        assertTrue(uiState is SurveyModel.SurveyUiState.Content)

        val contentState = uiState.contentUiState
        assertEquals(1, contentState.currentPage)
        assertEquals("Page 1", contentState.pageTitle)
        assertEquals("Page 1 Description", contentState.pageDescription)
    }

    @Test
    fun `advanceSurvey advances to next page or completes survey`() {
        val surveyConfig = SurveyConfig(
            title = "Test Survey",
            description = "Test Description",
            pages = listOf(
                SurveyPage("Page 1", "Page 1 Description", content = listOf()),
                SurveyPage("Page 2", "Page 2 Description", content = listOf())
            )
        )
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val surveyModel = SurveyModel(surveyConfig, File("build/tmp/test"), testScope)

        surveyModel.takeSurvey()
        surveyModel.advanceSurvey() // Should move to Page 2

        val page2State = surveyModel.surveyUiState
        assertTrue(page2State is SurveyModel.SurveyUiState.Content)
        val contentState = page2State.contentUiState
        assertEquals(2, contentState.currentPage)
        assertEquals("Page 2", contentState.pageTitle)

        surveyModel.advanceSurvey() // Should complete and reset to summary
        val summaryState = surveyModel.surveyUiState
        assertTrue(summaryState is SurveyModel.SurveyUiState.Summary)
    }

    @Test
    fun `updateAnswer updates text answer correctly`() {
        val surveyConfig = SurveyConfig(
            title = "Test Survey",
            description = "Test Description",
            pages = listOf(
                SurveyPage(
                    "Page 1",
                    "Page 1 Description",
                    content = listOf(
                        TextQuestion("Question 4", "Q4")
                    )
                )
            )
        )
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val surveyModel = SurveyModel(surveyConfig, File("build/tmp/test"), testScope)

        surveyModel.takeSurvey()
        surveyModel.updateAnswer("Q4", "Sample Answer")

        val surveyContent = surveyModel.surveyUiState
        assertTrue(surveyContent is SurveyModel.SurveyUiState.Content)
        val contentUiState = surveyContent.contentUiState

        val question1 = contentUiState.content.find { it.question.id == "Q4" } as? TextSurveyContentData
        assertNotNull(question1)
        assertEquals("Sample Answer", question1.answer)
    }

    @Test
    fun `updateAnswer updates likert answer correctly`() {
        val surveyConfig = SurveyConfig(
            title = "Test Survey",
            description = "Test Description",
            pages = listOf(
                SurveyPage(
                    "Page 1",
                    "Page 1 Description",
                    content = listOf(
                        LikertQuestion(
                            "Question 5",
                            "Q5",
                            statements = listOf(
                                LikertStatement("Statement 1"),
                                LikertStatement("Statement 2")
                            ),
                            choices = listOf("Strongly Disagree", "Disagree", "Neutral", "Agree", "Strongly Agree")
                        )
                    )
                )
            )
        )
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val surveyModel = SurveyModel(surveyConfig, File("build/tmp/test"), testScope)

        surveyModel.takeSurvey()
        surveyModel.updateAnswer("Q5", "Statement 1", "Agree")
        surveyModel.updateAnswer("Q5", "Statement 2", "Neutral")

        val surveyContent = surveyModel.surveyUiState
        assertTrue(surveyContent is SurveyModel.SurveyUiState.Content)
        val contentUiState = surveyContent.contentUiState

        val question5 = contentUiState.content.find { it.question.id == "Q5" } as? LikertSurveyContentData
        assertNotNull(question5)
        assertEquals("Agree", question5.answer?.get("Statement 1"))
        assertEquals("Neutral", question5.answer?.get("Statement 2"))
    }

    @Test
    fun `previous answers are Retrieved when navigating back`() {
        val surveyConfig = SurveyConfig(
            title = "Test Survey",
            description = "Test Description",
            pages = listOf(
                SurveyPage(
                    "Page 1",
                    "Page 1 Description",
                    content = listOf(
                        TextQuestion("Question 1", "Q1")
                    )
                ),
                SurveyPage(
                    "Page 2",
                    "Page 2 Description",
                    content = listOf(
                        TextQuestion("Question 2", "Q2")
                    )
                )
            )
        )
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val surveyModel = SurveyModel(surveyConfig, File("build/tmp/test"), testScope)

        surveyModel.takeSurvey()

        // Update answer on Page 1
        surveyModel.updateAnswer("Q1", "Answer 1")
        surveyModel.advanceSurvey() // Move to Page 2

        // Update answer on Page 2
        surveyModel.updateAnswer("Q2", "Answer 2")

        // Navigate back to Page 1
        surveyModel.backSurvey()

        val page1State = surveyModel.surveyUiState
        assertTrue(page1State is SurveyModel.SurveyUiState.Content)
        val contentState = page1State.contentUiState

        val question1 = contentState.content.find { it.question.id == "Q1" } as? TextSurveyContentData
        assertNotNull(question1)
        assertEquals("Answer 1", question1.answer)
    }
}
