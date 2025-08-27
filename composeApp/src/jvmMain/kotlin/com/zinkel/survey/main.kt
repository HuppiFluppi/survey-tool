package com.zinkel.survey

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.zinkel.survey.ui.SurveyApp
import com.zinkel.survey.ui.SurveyLoadApp
import com.zinkel.survey.ui.SurveyLoadModel
import com.zinkel.survey.ui.SurveyLoadUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.app_icon
import surveytool.composeapp.generated.resources.app_name

fun main() = application {
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
