package com.zinkel.survey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinkel.survey.FilePickerDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.app_icon
import surveytool.composeapp.generated.resources.app_name
import surveytool.composeapp.generated.resources.load_file
import surveytool.composeapp.generated.resources.load_file_error_text
import surveytool.composeapp.generated.resources.load_file_error_title

import surveytool.composeapp.generated.resources.ok
import java.io.File

@Composable
fun SurveyLoadScreen(surveyLoadUiState: SurveyLoadUiState, onFileSelected: (File) -> Unit) {
    var showFilePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(Res.string.app_name), fontSize = 32.sp, modifier = Modifier.padding(32.dp))
        Icon(painter = painterResource(Res.drawable.app_icon), contentDescription = null, modifier = Modifier.padding(16.dp))
        Button(onClick = { showFilePicker = true }, modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(Res.string.load_file))
        }
    }

    if (surveyLoadUiState is SurveyLoadUiState.Error) {
        val dismiss = { SurveyLoadModel.reset() }
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                TextButton(onClick = dismiss) {
                    Text(stringResource(Res.string.ok))
                }
            },
            title = {
                Text(stringResource(Res.string.load_file_error_title))
            },
            text = {
                Text(stringResource(Res.string.load_file_error_text, surveyLoadUiState.message))
            }
        )
    }

    if (showFilePicker && surveyLoadUiState is SurveyLoadUiState.NotLoaded) {
        FilePickerDialog(
            stringResource(Res.string.load_file),
            setOf("yaml", "yml"),
            onFileSelected = { showFilePicker = false; onFileSelected(it) },
            onCancel = { showFilePicker = false })
    }
}
