package com.zinkel.survey.ui.elements

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zinkel.survey.config.ChoiceItem
import com.zinkel.survey.config.ChoiceQuestion
import com.zinkel.survey.config.LikertQuestion
import com.zinkel.survey.config.LikertStatement
import com.zinkel.survey.config.NameQuestion
import com.zinkel.survey.config.RatingQuestion
import com.zinkel.survey.config.TextQuestion
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.points
import surveytool.composeapp.generated.resources.required
import surveytool.composeapp.generated.resources.star_filled
import surveytool.composeapp.generated.resources.star_unfilled

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

@Composable
@Preview
fun NameElementPreview() {
    NameElement(NameQuestion(title = "What is your name?", id = "1-1"))
}

@Composable
fun NameElement(question: NameQuestion) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
            titleRow(question.title, question.required, false, null)

            Row {
                var txt by remember(question.id) { mutableStateOf("") }
                TextFieldWithoutPadding(value = txt, onValueChange = { txt = it }, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.weight(0.5f))
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
        ), true
    )
}

@Composable
fun ChoiceElement(question: ChoiceQuestion, showQuestionScores: Boolean) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            titleRow(question.title, question.required, showQuestionScores, question.choices.sumOf { it.score ?: 0 })

            val checkStates = remember(question.id) { question.choices.map { it.title to false }.toMutableStateMap() }
            question.choices.forEach { choice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checkStates[choice.title]!!,
                        onCheckedChange = { handleChoiceChange(it, choice, question.multiple, question.limit, checkStates) })
                    Text(text = choice.title)
                }
            }
        }
    }
}

fun handleChoiceChange(checked: Boolean, choice: ChoiceItem, multiple: Boolean, limit: Int, checkStates: MutableMap<String, Boolean>) {
    if (limit > 0 && checked && checkStates.count { it.value } == limit) return // cant check more than limit
    if (!multiple && checkStates.any { it.value }) {
        checkStates.filter { it.value }.forEach { checkStates[it.key] = false }
    }
    checkStates[choice.title] = checked
}

@Composable
@Preview
fun LikertElementPreview() {
    LikertElement(
        LikertQuestion(
            "Likert Question", "1-1", choices = listOf("Strongly Disagree", "Disagree", "Neither Agree nor Disagree", "Agree", "Strongly Agree"),
            statements = listOf(LikertStatement("Banana", 1), LikertStatement("Apple", 1), LikertStatement("Orange", 1))
        ), true
    )
}

@Composable
fun LikertElement(question: LikertQuestion, showQuestionScores: Boolean) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            titleRow(question.title, question.required, showQuestionScores, question.statements.sumOf { it.score ?: 0 })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f))
                question.choices.forEach { choice ->
                    Text(text = choice, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                }
            }
            question.statements.forEach { statement ->
                val (selectedOption, onOptionSelected) = remember(question.id) { mutableStateOf<String?>(null) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = statement.title, modifier = Modifier.weight(1f))
                    question.choices.forEach { choice ->
                        RadioButton(selected = (choice == selectedOption), onClick = { onOptionSelected(choice) }, modifier = Modifier.weight(0.5f))
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun RatingElementPreview() {
    RatingElement(RatingQuestion(title = "How are you feeling today?", "1-1"), true)
}

@Composable
fun RatingElement(question: RatingQuestion, showQuestionScores: Boolean) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            titleRow(question.title, question.required, showQuestionScores, null)

            var rating by remember(question.id) { mutableStateOf(0) }
            val filledStar = painterResource(Res.drawable.star_filled)
            val unfilledStar = painterResource(Res.drawable.star_unfilled)
            Row {
                for (i in 1..5) {
                    val image = if (i <= rating) filledStar else unfilledStar
                    Icon(image, contentDescription = null, modifier = Modifier.padding(8.dp).clickable { rating = i })
                }
            }
        }
    }
}

@Composable
@Preview
fun TextElementPreview() {
    TextElement(TextQuestion(title = "How are you feeling today?", "1-1", score = 5), true)
}

@Composable
fun TextElement(question: TextQuestion, showQuestionScores: Boolean) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            titleRow(question.title, question.required, showQuestionScores, question.score)

            Row {
                var txt by remember(question.id) { mutableStateOf("") }
                TextFieldWithoutPadding(
                    value = txt,
                    onValueChange = { txt = it },
                    singleLine = !question.multiline,
                    minLines = if (question.multiline) 3 else 1,
                    maxLines = 6,
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
private fun titleRow(title: String, required: Boolean, showQuestionScores: Boolean, score: Int?) {
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

//need to duplicate TextField because content padding is fixed to 16.dp
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
