package com.zinkel.survey.config

import java.io.File

object SurveyConfigLoader {
    private val readers = mutableMapOf<String, SurveyConfigReader>()

    private fun addReader(reader: SurveyConfigReader, vararg extensions: String) {
        extensions.forEach {
            readers[it] = reader
        }
    }

    fun load(file: File): SurveyConfig {
        val reader = readers[file.extension] ?: throw IllegalArgumentException("Unsupported file format")
        return reader.loadConfig(file)
    }

    init {
        addReader(YamlReader(), "yaml", "yml")
    }
}

interface SurveyConfigReader {
    fun loadConfig(file: File): SurveyConfig
}

internal fun getContentId(pageNumber: Int, contentNumber: Int) = "$pageNumber-$contentNumber"
