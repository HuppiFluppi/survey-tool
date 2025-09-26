/**
 * Compose UI elements for rendering survey questions and related UI blocks.
 *
 * Contents:
 * - Page building blocks (headers, error banners).
 * - Question composables for Data, Text, Choice, Rating, and Likert types.
 * - Utility functions for selection handling and a padding-adjusted TextField.
 *
 * Design notes:
 * - Composables accept the corresponding configuration question type and callbacks to propagate state.
 * - Error display is driven by an optional inputError string passed by the caller.
 * - Score-related hints are optionally shown when enabled by the caller (e.g., for quizzes).
 */

package com.zinkel.survey.ui.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zinkel.survey.config.*
import com.zinkel.survey.data.DateTimePick
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import surveytool.composeapp.generated.resources.*
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * A composable function that displays a page header with an optional title and description.
 * If both, title and description are null, returns without adding composables.
 *
 * @param pageTitle The title of the page to be displayed. If null, the title will not be shown.
 * @param pageDescription The description of the page to be displayed. If null, the description will not be shown.
 */
@Composable
fun PageHeader(pageTitle: String?, pageDescription: String?) {
    if (pageTitle == null && pageDescription == null) return
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            pageTitle?.let { Text(text = it, fontWeight = FontWeight.Bold) }
            pageDescription?.let { Text(text = it) }
        }
    }
}

/**
 * Displays an animated error message header.
 *
 * If an error message is provided via the `inputError` parameter,
 * the header becomes visible, highlighting the error text.
 *
 * @param inputError The error message to display. If null, the header remains hidden.
 */
@Composable
fun ErrorHeader(inputError: String? = null) {
    AnimatedVisibility(inputError != null, modifier = Modifier.fillMaxWidth().background(color = MaterialTheme.colorScheme.error).padding(8.dp)) {
        Text(text = inputError ?: "", color = MaterialTheme.colorScheme.onError, textAlign = TextAlign.Center)
    }
}

@Composable
@Preview
fun DataElementPreview() {
    DataElement(DataQuestion(title = "What is your name?", id = "1-1"), {}, inputError = "This is an error")
}

/**
 * Renders a Data question with a single-line text field.
 *
 * Shows a title row with required indicator and an optional error banner.
 *
 * @param question The DataQuestion configuration to render.
 * @param onValueChange Callback invoked when the input changes.
 * @param savedValue Optional initial value to prefill the input.
 * @param inputError Optional error text to display above the card.
 */

@Composable
fun DataElement(question: DataQuestion, onValueChange: (String) -> Unit, savedValue: String = "", inputError: String? = null) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ErrorHeader(inputError)
        Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
            TitleRow(question.title, question.required, false, null)

            Row {
                var txt by remember(question.id) { mutableStateOf(savedValue) }
                TextFieldWithoutPadding(
                    value = txt,
                    label = when (question.dataType) {
                        DataQuestionType.NAME  -> @Composable { -> Text(stringResource(Res.string.data_element_label_name)) }
                        DataQuestionType.PHONE -> @Composable { -> Text(stringResource(Res.string.data_element_label_phone)) }
                        DataQuestionType.EMAIL -> @Composable { -> Text(stringResource(Res.string.data_element_label_email)) }
                        else                   -> null
                    },
                    trailingIcon = {
                        if (txt.isNotEmpty()) {
                            IconButton(onClick = { txt = ""; onValueChange("") }) {
                                Icon(painterResource(Res.drawable.clear), null)
                            }
                        }
                    },
                    onValueChange = { txt = it; onValueChange(it) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
@Preview
fun ChoiceElementPreview() {
    ChoiceElement(
        ChoiceQuestion(
            title = "What is your name?",
            id = "1-1",
            choices = listOf(ChoiceItem("first choice"), ChoiceItem("second choice"), ChoiceItem("third choice"))
        ), true, {}
    )
}

/**
 * Renders a Choice question supporting single- or multi-select options.
 *
 * Displays each option with a checkbox. For single-select, selecting an option clears any previous
 * selection. For multi-select, an optional limit is enforced by UI logic in [handleChoiceChange].
 *
 * @param question The ChoiceQuestion configuration to render.
 * @param showQuestionScores If true, shows aggregate potential score in the title row.
 * @param onValueChange Callback receiving the current list of selected option titles.
 * @param savedValues Optional initial selection.
 * @param inputError Optional error text to display above the card.
 */
@Composable
fun ChoiceElement(
    question: ChoiceQuestion,
    showQuestionScores: Boolean,
    onValueChange: (List<String>) -> Unit,
    savedValues: List<String> = emptyList(),
    inputError: String? = null,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ErrorHeader(inputError)
        Column(modifier = Modifier.padding(8.dp)) {
            TitleRow(question.title, question.required, showQuestionScores, question.choices.sumOf { it.score ?: 0 })

            if (question.dropdown && !question.multiple) { //Dropdown ui
                val chooseTxt = stringResource(Res.string.choice_element_choose)
                var expanded by remember(question.id) { mutableStateOf(false) }
                var choice by remember(question.id) { mutableStateOf(chooseTxt) }
                val dropDownRotate by animateFloatAsState(targetValue = if (expanded) 180f else 0f, animationSpec = tween())

                Row(modifier = Modifier.clickable { expanded = !expanded }) {
                    Text(choice)
                    Icon(painterResource(Res.drawable.drop_down), null, modifier = Modifier.graphicsLayer { rotationZ = dropDownRotate })

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        question.choices.forEach {
                            DropdownMenuItem(text = { Text(it.title) }, onClick = { expanded = false; choice = it.title; onValueChange(listOf(it.title)) })
                        }
                    }
                }
            } else { // default checkbox ui
                val checkStates = remember(question.id) { question.choices.map { it.title to (it.title in savedValues) }.toMutableStateMap() }
                question.choices.forEach { choice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checkStates[choice.title]!!,
                            onCheckedChange = { handleChoiceChange(it, choice, question.multiple, question.limit, checkStates, onValueChange) })
                        Text(text = choice.title)
                    }
                }
            }
        }
    }
}

/**
 * Updates selection state for Choice questions and emits the new selection list.
 *
 * Behavior:
 * - Enforces [limit] for multi-select by preventing additional checks once the limit is reached.
 * - For single-select ([multiple] == false), ensures only one option remains selected.
 *
 * @param checked New checked state for the interacted option.
 * @param choice The option being toggled.
 * @param multiple Whether multiple options can be selected.
 * @param limit Max number of selections when [multiple] is true; ignored when non-positive.
 * @param checkStates Mutable map of option title to selection state.
 * @param onValueChange Callback with the updated list of selected option titles.
 */
private fun handleChoiceChange(
    checked: Boolean,
    choice: ChoiceItem,
    multiple: Boolean,
    limit: Int,
    checkStates: MutableMap<String, Boolean>,
    onValueChange: (List<String>) -> Unit,
) {
    if (limit > 0 && checked && checkStates.count { it.value } == limit) return // cant check more than limit
    if (!multiple && checkStates.any { it.value }) {
        checkStates.filter { it.value }.forEach { checkStates[it.key] = false }
    }
    checkStates[choice.title] = checked
    onValueChange(checkStates.filter { it.value }.map { it.key })
}

@Composable
@Preview
fun LikertElementPreview() {
    LikertElement(
        LikertQuestion(
            "Likert Question", "1-1", choices = listOf("Strongly Disagree", "Disagree", "Neither Agree nor Disagree", "Agree", "Strongly Agree"),
            statements = listOf(LikertStatement("Banana", 1), LikertStatement("Apple", 1), LikertStatement("Orange", 1))
        ), true, { _, _ -> }
    )
}

/**
 * Renders a Likert scale question with multiple statements and uniform choices.
 *
 * Displays a header row of choices and, for each statement, a row of radio buttons.
 * Selected choices are remembered per statement and propagated via [onValueChange].
 *
 * @param question The LikertQuestion configuration to render.
 * @param showQuestionScores If true, shows total potential score from all statements.
 * @param onValueChange Callback receiving (statementTitle, choiceValue) for the latest selection.
 * @param savedValues Optional initial selections keyed by statement title.
 * @param inputError Optional error text to display above the card.
 */
@Composable
fun LikertElement(
    question: LikertQuestion,
    showQuestionScores: Boolean,
    onValueChange: (String, String) -> Unit,
    savedValues: Map<String, String> = emptyMap(),
    inputError: String? = null,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ErrorHeader(inputError)
        Column(modifier = Modifier.padding(8.dp)) {
            TitleRow(question.title, question.required, showQuestionScores, question.statements.sumOf { it.score ?: 0 })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f))
                question.choices.forEach { choice ->
                    Text(text = choice, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                }
            }
            question.statements.forEach { statement ->
                val (selectedOption, onOptionSelected) = remember(question.id) { mutableStateOf(savedValues[statement.title]) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = statement.title, modifier = Modifier.weight(1f))
                    question.choices.forEach { choice ->
                        RadioButton(
                            selected = (choice == selectedOption),
                            onClick = { onOptionSelected(choice); onValueChange(statement.title, choice) },
                            modifier = Modifier.weight(0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun RatingElementPreview() {
    RatingElement(RatingQuestion(title = "How are you feeling today?", "1-1"), {})
}

/**
 * Renders a 1..5 star rating ui component for a Rating question.
 *
 * Displays a title row and five clickable stars. Selection is remembered per question id
 * and propagated via [onValueChange].
 *
 * @param question The RatingQuestion configuration to render.
 * @param onValueChange Callback invoked with the selected rating (1..5).
 * @param savedValue Optional initial rating value (0 means no selection).
 * @param inputError Optional error text to display above the card.
 */
@Composable
fun RatingElement(question: RatingQuestion, onValueChange: (Int) -> Unit, savedValue: Int = 0, inputError: String? = null) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ErrorHeader(inputError)
        Column(modifier = Modifier.padding(8.dp)) {
            TitleRow(question.title, question.required, false, null)

            var rating by remember(question.id) { mutableStateOf(savedValue) }
            val (filledIcon, unfilledIcon) = when (question.symbol) {
                RatingSymbol.STAR  -> painterResource(Res.drawable.star_filled) to painterResource(Res.drawable.star_unfilled)
                RatingSymbol.HEART -> painterResource(Res.drawable.heart_filled) to painterResource(Res.drawable.heart_unfilled)
                RatingSymbol.LIKE  -> painterResource(Res.drawable.thumb_filled) to painterResource(Res.drawable.thumb_unfilled)
                RatingSymbol.SMILE -> painterResource(Res.drawable.smile_filled) to painterResource(Res.drawable.smile_unfilled)
            }
            val colorList = if (question.colorGradient != RatingColorGradient.NONE) rememberColorList(question.colorGradient, question.level) else null

            Row {
                for (i in 1..question.level) {
                    val image = if (i <= rating) filledIcon else unfilledIcon
                    Icon(
                        image,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).clickable { rating = i; onValueChange(i) },
                        tint = colorList?.get(i-1) ?: LocalContentColor.current
                    )
                }
            }
        }
    }
}

@Composable
fun rememberColorList(colorGradient: RatingColorGradient, level: Int) = remember(colorGradient, level) {
    val (start, end) = when (colorGradient) {
        RatingColorGradient.RED2GREEN -> Color.Red to Color.Green
        else                          -> throw IllegalArgumentException()
    }

    val redstep = (end.red - start.red) / (level - 1)
    val greenstep = (end.green - start.green) / (level - 1)
    val bluestep = (end.blue - start.blue) / (level - 1)

    (0..<level).map { Color(start.red + redstep * it, start.green + greenstep * it, start.blue + bluestep * it) }.toMutableStateList()
}

@Composable
@Preview
fun TextElementPreview() {
    TextElement(TextQuestion(title = "How are you feeling today?", "1-1", score = 5), true, {})
}

/**
 * Renders a Text question with a single- or multi-line text field.
 *
 * Displays a title row with optional score hint and an input field. The value is remembered
 * per question id and propagated via [onValueChange].
 *
 * @param question The TextQuestion configuration to render.
 * @param showQuestionScores If true, shows the potential score in the header (quizzes).
 * @param onValueChange Callback invoked when the input changes.
 * @param savedValue Optional initial text.
 * @param inputError Optional error text to display above the card.
 */
@Composable
fun TextElement(question: TextQuestion, showQuestionScores: Boolean, onValueChange: (String) -> Unit, savedValue: String = "", inputError: String? = null) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ErrorHeader(inputError)
        Column(modifier = Modifier.padding(8.dp)) {
            TitleRow(question.title, question.required, showQuestionScores, question.score)

            Row {
                var txt by remember(question.id) { mutableStateOf(savedValue) }
                TextFieldWithoutPadding(
                    value = txt,
                    onValueChange = { txt = it; onValueChange(it) },
                    trailingIcon = {
                        if (txt.isNotEmpty()) {
                            IconButton(onClick = { txt = ""; onValueChange("") }) {
                                Icon(painterResource(Res.drawable.clear), null)
                            }
                        }
                    },
                    singleLine = !question.multiline,
                    minLines = if (question.multiline) 3 else 1,
                    maxLines = 6,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
@Preview
fun DateTimeElementPreview() {
    DateTimeElement(DateTimeQuestion(title = "Pick a date and time", "1-1", score = 5), true, {})
}

/**
 * Renders a DateTime question.
 *
 * Displays a title row with optional score hint together with a date and/or time input.
 *
 * @param question The DateTimeQuestion configuration to render.
 * @param showQuestionScores If true, shows the potential score in the header (quizzes).
 * @param onValueChange Callback invoked when the input changes.
 * @param savedValue Optional initial value.
 * @param inputError Optional error text to display above the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeElement(
    question: DateTimeQuestion,
    showQuestionScores: Boolean,
    onValueChange: (DateTimePick) -> Unit,
    savedValue: DateTimePick? = null,
    inputError: String? = null,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ErrorHeader(inputError)

        Column(modifier = Modifier.padding(8.dp)) {
            TitleRow(question.title, question.required, showQuestionScores, question.score)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                val datePick by remember(question.id) { mutableStateOf(savedValue ?: DateTimePick(question.initialSelectedDate, question.initialSelectedTime)) }

                // Date
                if (question.inputType == DateTimeType.DATE || question.inputType == DateTimeType.DATETIME) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = datePick.date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                        initialDisplayMode = DisplayMode.Input,
                    )

                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier.weight(1f, false).clip(MaterialTheme.shapes.medium),
                        title = null
                    )

                    LaunchedEffect(datePickerState.selectedDateMillis) {
                        datePick.date = datePickerState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                        onValueChange(datePick)
                    }
                }

                if (question.inputType == DateTimeType.DATETIME) Spacer(modifier = Modifier.width(32.dp))

                // Time
                if (question.inputType == DateTimeType.TIME || question.inputType == DateTimeType.DATETIME) {
                    val timePickerState = rememberTimePickerState(
                        initialHour = datePick.time?.hour ?: 0,
                        initialMinute = datePick.time?.minute ?: 0,
                    )

                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                            .weight(1.5f, false)
                    ) {
                        TimeInput(state = timePickerState)
                    }

                    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                        datePick.time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        onValueChange(datePick)
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun InformationBlockElementPreview() {
    InformationBlockElement(
        InformationBlock(
            title = "Mighty information block",
            "1-1",
            "The description is descriptive as such. Only a description like this brings true description joy. It's just so describable!",
        )
    )
}

/**
 * A composable function that displays an information block with an optional description and image.
 *
 * @param block The [Information block] holding information about title, description and image of the element to be displayed.
 */
@Composable
fun InformationBlockElement(block: InformationBlock) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = block.title, fontWeight = FontWeight.Bold)
            block.description?.let { Text(text = it, modifier = Modifier.padding(top = 8.dp)) }
            block.image?.let {
                val image = remember(block.image) { loadImageBitmap(it) }
                Image(image, contentDescription = null, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }
}

fun loadImageBitmap(file: File) = file.readBytes().decodeToImageBitmap()

/**
 * Renders the title row for a question card.
 *
 * Shows the question title at the left and optional indicators at the right:
 * - A "required" marker when applicable.
 * - A score hint when [showQuestionScores] is true and [score] is provided.
 *
 * @param title Question title.
 * @param required Whether the question must be answered.
 * @param showQuestionScores Whether score hints should be displayed.
 * @param score Optional score value to display.
 */
@Composable
private fun TitleRow(title: String, required: Boolean, showQuestionScores: Boolean, score: Int?) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(text = title)

        val optionItems = mutableListOf<String>()
        if (required) optionItems.add(stringResource(Res.string.required))
        if (showQuestionScores && score != null) optionItems.add(stringResource(Res.string.points, score))

        if (optionItems.isNotEmpty()) {
            val text = optionItems.joinToString(" - ", prefix = "*")
            Text(
                text = text,
                modifier = Modifier.padding(start = 8.dp).alpha(0.5f),
            )
        }
    }
}

/**
 * TextField variant with adjustable content padding.
 *
 * Based on Material3 TextField but exposes [contentPadding] as content padding is fixed to 16.dp.
 * All other behavior mirrors the standard composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextFieldWithoutPadding(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    contentPadding: PaddingValues = PaddingValues(
        top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp
    ),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            when {
                !enabled -> colors.disabledTextColor
                isError  -> colors.errorTextColor
                focused  -> colors.focusedTextColor
                else     -> colors.unfocusedTextColor
            }
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier = modifier,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox =
                @Composable { innerTextField ->
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.DecorationBox(
                        value = value,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = label,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        prefix = prefix,
                        suffix = suffix,
                        supportingText = supportingText,
                        shape = shape,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        contentPadding = contentPadding
                    )
                }
        )
    }
}
