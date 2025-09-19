package com.zinkel.survey.config

import java.io.File

/**
 * Central registry and facade for loading [SurveyConfig] from files.
 *
 * Supports pluggable readers keyed by file extension. By default, YAML files
 * ("yaml", "yml") are supported via [YamlReader].
 *
 * Usage:
 * - Call [load] with a file; the appropriate [SurveyConfigReader] is resolved by its extension.
 * - Extend supported formats by registering additional readers inside [init].
 *
 * Thread-safety:
 * - Registration occurs during initialization; runtime lookups are read-only.
 */
object SurveyConfigLoader {
    private val readers = mutableMapOf<String, SurveyConfigReader>()

    /**
     * Registers a [SurveyConfigReader] for one or more file [extensions].
     *
     * @param reader Reader capable of parsing the target format.
     * @param extensions File extensions without a leading dot (e.g., "yaml", "json").
     */
    private fun addReader(reader: SurveyConfigReader, vararg extensions: String) {
        extensions.forEach {
            readers[it] = reader
        }
    }

    val validExtensions: Set<String>
        get() = readers.keys.toSet()

    /**
     * Loads a [SurveyConfig] from the provided [file].
     *
     * The loader dispatches to a registered reader based on [File.extension].
     *
     * @throws IllegalArgumentException if no reader is registered for the file extension.
     */
    fun load(file: File): SurveyConfig {
        val reader = readers[file.extension] ?: throw IllegalArgumentException("Unsupported file format")
        return reader.loadConfig(file)
    }

    init {
        addReader(YamlReader(), "yaml", "yml")
    }
}

/**
 * Interface for parsing a [SurveyConfig] from a file.
 *
 * Implementations should validate the input and throw an exception on malformed content.
 */
interface SurveyConfigReader {
    fun loadConfig(file: File): SurveyConfig
}

/**
 * Utility to generate a stable content identifier within a survey.
 *
 * The identifier is a simple "pageNumber-contentNumber" composite string.
 * It is useful for mapping answers, validation messages, and UI state to a unique key.
 * The id of a questio should be unique within a page and created once.
 *
 * @return A composite identifier in the form "pageNumber-contentNumber" (e.g., "2-3").
 */
internal fun getContentId(pageNumber: Int, contentNumber: Int) = "$pageNumber-$contentNumber"
