package com.zinkel.survey

import androidx.compose.runtime.Composable
import java.io.File

expect fun getPlatform(): String

@Composable
expect fun FilePickerDialog(title: String, extensions: Set<String> = emptySet(), onFileSelected: (File) -> Unit, onCancel: () -> Unit)
