package com.zinkel.survey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinkel.survey.FilePickerDialog
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import surveytool.composeapp.generated.resources.*
import java.io.File
import java.net.URI

/**
 * A composable function that displays the Survey Load Screen UI. This screen allows users to load a survey
 * configuration file either through a file picker or drag-and-drop functionality. It also handles various
 * states of the UI during the file loading process, such as file selection, drag-and-drop, and errors.
 *
 * @param surveyLoadUiState Describes the current state of the survey loading process. It provides information
 * about whether the data is not loaded, loading, successfully loaded, or encountered an error.
 * @param onFileSelected Callback invoked when a valid file is selected or dropped. It accepts a `File` object
 * representing the selected survey configuration file.
 */
@Composable
fun SurveyLoadScreen(surveyLoadUiState: SurveyLoadUiState, onFileSelected: (File) -> Unit) {
    val validExtensions = remember { setOf("yaml", "yml") }
    var showFilePicker by remember { mutableStateOf(false) }
    val showDragAndDropIndicatorState = remember { mutableStateOf(false) }
    var fileSelectionResult by remember { mutableStateOf<FileSelectionResult?>(null) }
    val dragAndDropTarget = rememberDragAndDropTarget(
        showDragAndDropIndicatorState,
        validExtensions,
        { fileSelectionResult = FileSelectionResult(true, it) },
        { fileSelectionResult = FileSelectionResult(false) })

    Box(Modifier.dragAndDropTarget(shouldStartDragAndDrop = { fileSelectionResult == null }, target = dragAndDropTarget)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 32.dp)
            )
            Icon(
                painter = painterResource(Res.drawable.app_icon),
                tint = Color.Unspecified,
                contentDescription = "app icon",
                modifier = Modifier.padding(16.dp)
            )
            Button(onClick = { showFilePicker = true }, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(Res.string.load_file))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${stringResource(Res.string.app_version)} (${stringResource(Res.string.app_build)})",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Drag and drop indicator
        if (showDragAndDropIndicatorState.value || fileSelectionResult != null) {
            Card(modifier = Modifier.fillMaxSize().padding(16.dp).alpha(0.9f), shape = CutCornerShape(0.dp)) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    when (fileSelectionResult?.valid) {
                        true -> Icon(painter = painterResource(Res.drawable.file_drop_success), contentDescription = null, tint = Color.Green)
                        false -> Icon(painter = painterResource(Res.drawable.file_drop_error), contentDescription = null, tint = Color.Red)
                        else -> {
                            Icon(painter = painterResource(Res.drawable.file_drop_indicator), contentDescription = null)
                            Text(stringResource(Res.string.drop_file), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (fileSelectionResult != null) {
                        LaunchedEffect(fileSelectionResult) {
                            delay(800)
                            if (fileSelectionResult!!.valid) onFileSelected(fileSelectionResult!!.file!!)
                            delay(800)
                            fileSelectionResult = null
                        }
                    }
                }
            }
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
            validExtensions,
            onFileSelected = { showFilePicker = false; fileSelectionResult = FileSelectionResult(true, it) },
            onCancel = { showFilePicker = false })
    }
}

private data class FileSelectionResult(val valid: Boolean, val file: File? = null) {
    init {
        require(!valid || (valid && file != null))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun rememberDragAndDropTarget(
    showDragAndDropIndicatorState: MutableState<Boolean>,
    validExtensions: Set<String>,
    onFileSelected: (File) -> Unit,
    onDropRejected: () -> Unit,
) = remember {
    object : DragAndDropTarget {

        override fun onDrop(event: DragAndDropEvent): Boolean {
            val data = event.dragData()
            if (!checkDrop(data)) {
                onDropRejected()
                return false
            } else return true
        }

        private fun checkDrop(data: DragData): Boolean {
            if (data is DragData.FilesList) {
                val files = data.readFiles()

                if (files.size == 1) {
                    val file = File(URI(files.first()))

                    if (file.extension !in validExtensions) {
                        println("Invalid file extension: ${files.first()}")
                        return false
                    }

                    println("File dropped: ${files.first()}")
                    onFileSelected(file)
                    return true
                } else {
                    println("Unsupported number of dragged files: ${files.size}")
                    return false
                }
            } else {
                println("Unsupported drag and drop data: $data")
                return false
            }
        }

        override fun onStarted(event: DragAndDropEvent) {
            showDragAndDropIndicatorState.value = true
        }

        override fun onEnded(event: DragAndDropEvent) {
            showDragAndDropIndicatorState.value = false
        }
    }
}
