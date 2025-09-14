package com.zinkel.survey.ui.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zinkel.survey.ui.HighscoreEntry
import com.zinkel.survey.ui.HighscoreUiState
import org.jetbrains.compose.resources.stringResource
import surveytool.composeapp.generated.resources.Res
import surveytool.composeapp.generated.resources.highscore

/**
 * Displays the high score leaderboard in a composable layout.
 *
 * This function renders a leaderboard card that lists the top scores
 * based on the provided `HighscoreUiState`. It formats each entry
 * and shows a limited number of scores determined by the `limit`
 * parameter specified in the `HighscoreUiState`.
 *
 * @param highscoreUiState The UI state containing the leaderboard data, including the list of scores,
 *                         display limit, and whether to show the scores alongside names.
 */
@Composable
fun HighScore(highscoreUiState: HighscoreUiState) {
    ElevatedCard(modifier = Modifier.padding(16.dp)) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text(text = stringResource(Res.string.highscore), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp))

            highscoreUiState.scores.sortedByDescending { it.score }.take(highscoreUiState.limit).forEachIndexed { index, it ->
                HighscoreElement(index + 1, it, highscoreUiState.showScores)
            }
        }
    }
}

/**
 * Displays a single highscore element as part of [HighScore].
 *
 * @param position The rank or position of the entry in the leaderboard.
 * @param highscoreEntry The data representing a single highscore entry, including name and score.
 * @param showScores A flag indicating whether to display the score alongside the participant's name.
 */
@Composable
fun HighscoreElement(position: Int, highscoreEntry: HighscoreEntry, showScores: Boolean) {
    if (showScores) Text(text = "${position}. ${highscoreEntry.name} - ${highscoreEntry.score}")
    else Text(text = "${position}. ${highscoreEntry.name}")
}
