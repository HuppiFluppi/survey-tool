package com.zinkel.survey

import androidx.compose.runtime.Composable
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView

actual fun getPlatform(): String = "JVM"

@Composable
actual fun FilePickerDialog(title: String, extensions: Set<String>, onFileSelected: (File) -> Unit, onCancel: () -> Unit) {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    val fileChooser = JFileChooser(FileSystemView.getFileSystemView())
    fileChooser.dialogTitle = title
    fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
    fileChooser.isAcceptAllFileFilterUsed = true
    fileChooser.currentDirectory = null
    fileChooser.isAcceptAllFileFilterUsed = extensions.isEmpty()
    extensions.forEach {
        fileChooser.addChoosableFileFilter(FileNameExtensionFilter(it.removePrefix(".").uppercase(), it.removePrefix(".")))
    }

    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        val file = fileChooser.selectedFile
        println("choosen file: $file")
        onFileSelected(file)
    } else {
        println("file selection cancelled")
        onCancel()
    }
}
