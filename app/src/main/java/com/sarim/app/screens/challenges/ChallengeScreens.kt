package com.sarim.app.screens.challenges

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sarim.app.R
import kotlin.random.Random

@Composable
fun ChallengeScaffold(
    step: Int,
    total: Int,
    onGiveUp: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (total > 1) {
            Text(
                stringResource(R.string.challenge_step, step, total),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
        Button(
            onClick = onGiveUp,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp)
        ) {
            Text(stringResource(R.string.challenge_give_up), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun MathChallenge(onSuccess: () -> Unit) {
    var problemIndex by remember { mutableIntStateOf(0) }
    var num1 by remember { mutableIntStateOf(Random.nextInt(10, 99)) }
    var num2 by remember { mutableIntStateOf(Random.nextInt(10, 99)) }
    var answer by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val totalProblems = 5

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.math_challenge_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("${problemIndex + 1} / $totalProblems", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("$num1 + $num2 =", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = answer,
            onValueChange = { 
                answer = it
                isError = false
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isError,
            supportingText = {
                if (isError) Text(stringResource(R.string.challenge_error))
            }
        )
        
        Button(
            onClick = {
                val result = num1 + num2
                if (answer.toIntOrNull() == result) {
                    if (problemIndex + 1 >= totalProblems) {
                        onSuccess()
                    } else {
                        problemIndex++
                        num1 = Random.nextInt(10, 99)
                        num2 = Random.nextInt(10, 99)
                        answer = ""
                    }
                } else {
                    isError = true
                    problemIndex = 0
                    num1 = Random.nextInt(10, 99)
                    num2 = Random.nextInt(10, 99)
                    answer = ""
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(stringResource(R.string.confirm))
        }
    }
}
