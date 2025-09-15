package com.zinkel.survey

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.zinkel.survey.config.RatingQuestion
import com.zinkel.survey.config.SurveyConfig
import com.zinkel.survey.config.SurveyPage
import com.zinkel.survey.config.TextQuestion
import com.zinkel.survey.ui.SurveyApp
import com.zinkel.survey.ui.SurveyLoadApp
import com.zinkel.survey.ui.SurveyLoadModel
import com.zinkel.survey.ui.SurveyLoadUiState
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.app_name
import surveytool.composeapp.generated.resources.cancel
import surveytool.composeapp.generated.resources.finish
import surveytool.composeapp.generated.resources.load_file
import surveytool.composeapp.generated.resources.take_survey
import java.io.File
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppLoadTest {

    @Test
    fun `SurveyLoadApp can be instantiated`() = runBlocking {
        val appName = getString(Res.string.app_name)
        val loadButtonText = getString(Res.string.load_file)

        runComposeUiTest {
            val surveyLoadUiState = SurveyLoadModel.surveyLoadUiState
            SurveyLoadModel.reset()

            setContent { SurveyLoadApp(surveyLoadUiState) }

            onNodeWithText(appName).assertExists()
            onNodeWithContentDescription("app icon").assertExists()
            onNodeWithText(loadButtonText).assertExists()
        }
    }

    @Test
    fun `SurveyApp summary can be instantiated`() = runBlocking {
        val startSurveyText = getString(Res.string.take_survey)

        runComposeUiTest {
            val surveyLoadUiState = SurveyLoadUiState.Loaded(
                SurveyConfig(
                    title = "Test Survey",
                    description = "Test Description",
                    pages = listOf()
                ),
                File("")
            )

            setContent { SurveyApp(surveyLoadUiState) }

            onNodeWithText(surveyLoadUiState.config.title).assertExists()
            onNodeWithText(surveyLoadUiState.config.description).assertExists()
            onNodeWithText(startSurveyText).assertExists()
        }
    }

    @Test
    fun `SurveyApp content can be instantiated`() = runBlocking {
        val startSurveyText = getString(Res.string.take_survey)
        val cancelButtonText = getString(Res.string.cancel)
        val finishButtonText = getString(Res.string.finish)

        runComposeUiTest {
            val surveyLoadUiState = SurveyLoadUiState.Loaded(
                SurveyConfig(
                    title = "Test Survey",
                    description = "Test Description",
                    pages = listOf(
                        SurveyPage(content = listOf(TextQuestion("Question 1", "Q1"), RatingQuestion("Rating Question", "Q2")))
                    )
                ),
                File("")
            )

            setContent { SurveyApp(surveyLoadUiState) }

            onNodeWithText(startSurveyText).performClick()

            onNodeWithText(surveyLoadUiState.config.title).assertExists()

            onNodeWithText(surveyLoadUiState.config.pages[0].content[0].title).assertExists()
            onNodeWithText(surveyLoadUiState.config.pages[0].content[1].title).assertExists()

            onNodeWithText(cancelButtonText).assertExists()
            onNodeWithText(finishButtonText).assertExists()
        }
    }
}
