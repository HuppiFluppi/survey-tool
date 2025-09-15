package com.zinkel.survey.ui

import com.zinkel.survey.config.SurveyConfig
import com.zinkel.survey.config.SurveyConfigLoader
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SurveyLoadStateTest {

    @BeforeTest
    fun setUp() {
        SurveyLoadModel.reset()
    }

    @Test
    fun `test load function sets state to Loading initially`() = runBlocking {
        // Arrange
        val mockFile = mockk<File>()
        val mockConfig = mockk<SurveyConfig>()
        mockkObject(SurveyConfigLoader)
        every { SurveyConfigLoader.load(mockFile) } answers { Thread.sleep(50); mockConfig }

        // Act
        val job = launch { SurveyLoadModel.load(mockFile) }
        delay(20)

        // Assert
        assertEquals(SurveyLoadUiState.Loading, SurveyLoadModel.surveyLoadUiState)

        job.join()

        // Clean up
        unmockkObject(SurveyConfigLoader)
    }

    @Test
    fun `test load function sets state to Loaded when config is loaded successfully`() = runBlocking {
        // Arrange
        val mockFile = mockk<File>()
        val mockConfig = mockk<SurveyConfig>()
        mockkObject(SurveyConfigLoader)
        every { SurveyConfigLoader.load(mockFile) } returns mockConfig

        // Act
        SurveyLoadModel.load(mockFile)

        // Assert
        assertEquals(SurveyLoadUiState.Loaded(mockConfig, mockFile), SurveyLoadModel.surveyLoadUiState)

        // Clean up
        unmockkObject(SurveyConfigLoader)
    }

    @Test
    fun `test load function sets state to Error when an exception occurs`() = runBlocking {
        // Arrange
        val mockFile = mockk<File>()
        val errorMessage = "Test exception"
        mockkObject(SurveyConfigLoader)
        every { SurveyConfigLoader.load(mockFile) } throws RuntimeException(errorMessage)

        // Act
        SurveyLoadModel.load(mockFile)

        // Assert
        assertEquals(SurveyLoadUiState.Error(errorMessage), SurveyLoadModel.surveyLoadUiState)

        // Clean up
        unmockkObject(SurveyConfigLoader)
    }

    @Test
    fun `test reset function sets state to NotLoaded`() {
        // Act
        SurveyLoadModel.reset()

        // Assert
        assertEquals(SurveyLoadUiState.NotLoaded, SurveyLoadModel.surveyLoadUiState)
    }
}
