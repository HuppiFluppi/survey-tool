package com.zinkel.survey

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.zinkel.survey.ui.SurveyApp
import com.zinkel.survey.ui.SurveyLoadModel
import com.zinkel.survey.ui.SurveyLoadUiState.Loaded
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.DensityQualifier
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.ThemeQualifier
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import surveytool.composeapp.generated.resources.Res.string
import surveytool.composeapp.generated.resources.cancel
import surveytool.composeapp.generated.resources.finish
import surveytool.composeapp.generated.resources.take_quiz
import java.io.File
import java.nio.file.Files
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class, InternalResourceApi::class)
class SystemTest {

    // getString() fetches the current ResourceEnvironment to know which language to load.
    // This crashes on environments w/o GUI (e.g. pipelines, containers, etc.)
    // While the getString() can be given a own ResourceEnvironment, this class is internal. Hence, the mocking and init block.
    @OptIn(InternalResourceApi::class)
    val testEnv = mockk<ResourceEnvironment>()

    init {
        mockkStatic(::getSystemResourceEnvironment)
        every { getSystemResourceEnvironment() } returns testEnv

        every { testEnv getProperty "language" } returns LanguageQualifier(Locale.getDefault().language)
        every { testEnv getProperty "region" } returns RegionQualifier("US")
        every { testEnv getProperty "theme" } returns ThemeQualifier.DARK
        every { testEnv getProperty "density" } returns DensityQualifier.HDPI
    }

    @Test
    fun `E2E test with test survey config yaml`() = runBlocking {
        val tempDir = Files.createTempDirectory("system_test").toFile()
        val configFile = File(tempDir, "system_test_survey.yaml")

        try {
            //setup config/data files & directory
            this::class.java.getResourceAsStream("/system_test_survey.yaml")!!.use { input ->
                configFile.outputStream().use { input.copyTo(it) }
            }

            //load survey config
            SurveyLoadModel.load(configFile)
            assertIs<Loaded>(SurveyLoadModel.surveyLoadUiState)

            // button texts
            val startQuizText = getString(testEnv, string.take_quiz)
            val finishSurveyText = getString(testEnv, string.finish)
            val cancelSurveyText = getString(testEnv, string.cancel)

            fun getSemanticRoleMatcher(role: Role) = SemanticsMatcher.keyIsDefined(SemanticsProperties.Role) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

            // values from loaded config (keep in sync with system_text_survey.yaml
            val surveyTitle = "Test Survey"
            val firstPageTitle = "First Page"
            val nameQuestionText = "Give me your name"
            val infoBlockTitle = "This is non question content"
            val choiceTitle = "What is your favorite weather?"
            val conditionalTextTitle = "What is your favorite ice cream?"
            val dateTimeTitle = "When was this feature created?"
            val sliderTitle = "What is your favorite temperature?"
            val ratingTitle = "How is your mood?"
            val likertTitle = "What tv/movie genres you like?"

            // values to set
            val insertName = "Test"
            val insertWeather = "Wind"
            val insertIcecream = "Pistachio"
            val insertTemp = 12.0f
            val insertMood = 3
            val insertLikert = "Love it"

            runComposeUiTest {
                setContent { SurveyApp(SurveyLoadModel.surveyLoadUiState as Loaded) }

                // Check initial Summary page
                onNodeWithText(surveyTitle).assertExists("Survey title $surveyTitle should be visible")
                onNodeWithText(startQuizText).assertExists("Start quiz button should be visible")
                onNodeWithText(startQuizText).performClick()

                // Check Survey content page
                onNodeWithText(firstPageTitle).assertExists("Page title should be visible")
                onNodeWithText(nameQuestionText).assertExists("Name question should be visible")
                onNodeWithText(infoBlockTitle).assertExists("Info block should be visible")
                onNodeWithText(choiceTitle).assertExists("Weather choice should be visible")
                onNodeWithText(conditionalTextTitle).assertDoesNotExist()
                onNodeWithText(dateTimeTitle).assertExists("Date element should be visible")
                onNodeWithText(sliderTitle).assertExists("Temperature slider should be visible")
                onNodeWithText(ratingTitle).assertExists("Mood rating should be visible")
                onNodeWithText(likertTitle).assertExists("Genre likert should be visible")
                onNodeWithText(cancelSurveyText).assertExists("Cancel button should be visible")
                onNodeWithText(finishSurveyText).assertExists("Finish button should be visible")

                // Set values for questions/elements
                onNodeWithText(nameQuestionText).onSiblings().filterToOne(hasSetTextAction()).performTextInput(insertName)
                onNodeWithText(choiceTitle).onSiblings().filterToOne(hasAnyChild(getSemanticRoleMatcher(Role.RadioButton))).onChildren()
                    .filterToOne(hasText(insertWeather)).performScrollTo().performClick()
                onNodeWithText(sliderTitle).onSiblings().filterToOne(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
                    .performSemanticsAction(SemanticsActions.SetProgress) { it(insertTemp) }
                onNodeWithContentDescription("rating_option_$insertMood").performScrollTo().performClick()
                val likertNodes = onNodeWithText(likertTitle).onSiblings().filter(hasContentDescription(insertLikert))
                likertNodes.fetchSemanticsNodes().forEachIndexed { i, _ -> likertNodes[i].performScrollTo().performClick() }

                // Check conditional element visibility
                onNodeWithText(conditionalTextTitle).assertExists("Conditional text should now be visible")

                // Set conditional element text value
                onNodeWithText(conditionalTextTitle).onSiblings().filterToOne(hasSetTextAction()).performTextInput(insertIcecream)

                // Finish survey
                onNodeWithText(finishSurveyText).performClick()

                // Check Summary page
                onNodeWithText(startQuizText).assertExists("Start quiz button should be visible")
                onNodeWithText(surveyTitle).assertExists("Survey title $surveyTitle should be visible")

                delay(200.milliseconds)
            }

            // Check CSV output
            val dataFile = File(tempDir, "${configFile.nameWithoutExtension}_data.csv")
            val summaryFile = File(tempDir, "${configFile.nameWithoutExtension}_summary.csv")

            assertTrue(dataFile.exists(), "Data CSV file should exist")
            assertTrue(summaryFile.exists(), "Summary CSV file should exist")

            // Verify data file contents
            val dataRows = csvReader { autoRenameDuplicateHeaders = true; skipEmptyLine = true }.readAllWithHeader(dataFile)
            assertEquals(1, dataRows.size, "Should have one data row")
            assertEquals("1", dataRows[0]["#"])
            assertEquals("QUIZ", dataRows[0]["Survey Type"])
            // - check answers
            assertEquals(insertName, dataRows[0]["User"])
            assertEquals(insertName, dataRows[0][nameQuestionText])
            assertEquals(insertWeather, dataRows[0][choiceTitle])
            assertEquals(insertIcecream, dataRows[0][conditionalTextTitle])
            assertTrue(dateTimeTitle in dataRows[0])
            assertEquals(insertTemp.toString(), dataRows[0][sliderTitle])
            assertEquals(insertMood.toString(), dataRows[0][ratingTitle])
            val likertRegex = "\\{Horror=${insertLikert}, Comedy=${insertLikert}, Drama=${insertLikert}, Action=${insertLikert}\\}".toRegex()
            assertTrue(dataRows[0][likertTitle]!!.matches(likertRegex))

            // Verify summary file contents
            val summaryRows = csvReader { skipEmptyLine = true }.readAllWithHeader(summaryFile)
            assertEquals(1, summaryRows.size)
            assertEquals("1", summaryRows[0]["Submitted Count"])
            assertEquals("QUIZ", summaryRows[0]["Survey Type"])
            assertEquals(surveyTitle, summaryRows[0]["Survey Title"])
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
