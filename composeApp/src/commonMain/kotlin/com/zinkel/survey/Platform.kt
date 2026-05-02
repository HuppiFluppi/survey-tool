package com.zinkel.survey

import androidx.compose.runtime.Composable
import java.io.File

@Composable
expect fun FilePickerDialog(title: String, extensions: Set<String> = emptySet(), onFileSelected: (File) -> Unit, onCancel: () -> Unit)
