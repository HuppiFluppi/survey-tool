package com.zinkel.survey

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.zinkel.survey.config.SurveyConfigLoader.validExtensions
import com.zinkel.survey.ui.SurveyApp
import com.zinkel.survey.ui.SurveyLoadApp
import com.zinkel.survey.ui.SurveyLoadModel
import com.zinkel.survey.ui.SurveyLoadUiState
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.app_icon
import surveytool.composeapp.generated.resources.app_name
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) = application {
    checkArgs(args) // check cmd line params

    val surveyLoadUiState = SurveyLoadModel.surveyLoadUiState

    if (surveyLoadUiState is SurveyLoadUiState.Loaded) {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            resizable = false,
            icon = painterResource(Res.drawable.app_icon),
            state = rememberWindowState(width = 1200.dp, height = 1000.dp)
        ) {
            SurveyApp(surveyLoadUiState)
        }
    } else {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            resizable = false,
            icon = painterResource(Res.drawable.app_icon),
            state = rememberWindowState(width = 500.dp, height = 500.dp)
        ) {
            SurveyLoadApp(surveyLoadUiState)
        }
    }
}

private fun ApplicationScope.checkArgs(args: Array<String>) {
    if (args.isNotEmpty()) {
        val config = args.find { !it.startsWith("-") }?.let { File(it) }
        if ("-h" in args || "--help" in args || config == null) {
            println("Survey Tool")
            println("===========")
            println("Usage: java -jar surveytool.jar [options] <survey_config>")
            println()
            println("  arguments: ")
            println("    <survey_config>: path to a survey config file (absolute or relative)")
            println()
            println("  options: ")
            println("    -h, --help: show this help message and exit")
            println()
            exitApplication()
            exitProcess(0)
        }

        if (!config.exists() || config.extension !in validExtensions) {
            println("File does not exist or invalid file extension: $config")
            exitApplication()
            exitProcess(1)
        }

        runBlocking { SurveyLoadModel.load(config) }
    }
}
