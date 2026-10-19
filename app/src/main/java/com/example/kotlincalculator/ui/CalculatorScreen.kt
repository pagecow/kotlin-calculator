package com.example.kotlincalculator.ui

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlincalculator.calculator.CalculatorState
import com.example.kotlincalculator.calculator.Operation
import com.example.kotlincalculator.ui.theme.CalculatorOrange
import com.example.kotlincalculator.ui.theme.KotlinCalculatorTheme

private val KeyHeight = 72.dp
private val KeyGap = 12.dp

/**
 * The screen: a big display on top of a 4-column keypad.
 *
 * The state survives configuration changes (rotation, dark-mode switches)
 * because [CalculatorState] is `Serializable` and remembered with
 * [rememberSaveable].
 */
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    var state by rememberSaveable { mutableStateOf(CalculatorState()) }

    CalculatorContent(
        state = state,
        onStateChange = { state = it },
        modifier = modifier,
    )
}

@Composable
private fun CalculatorContent(
    state: CalculatorState,
    onStateChange: (CalculatorState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Display(
                text = state.display,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            Keypad(state = state, onStateChange = onStateChange)
        }
    }
}

// MARK: - Display

@Composable
private fun Display(text: String, modifier: Modifier = Modifier) {
    // Long numbers shrink instead of being clipped.
    val fontSize = when {
        text.length > 16 -> 40.sp
        text.length > 12 -> 52.sp
        text.length > 9 -> 64.sp
        else -> 76.sp
    }

    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = fontSize,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// MARK: - Keypad

@Composable
private fun Keypad(
    state: CalculatorState,
    onStateChange: (CalculatorState) -> Unit,
) {
    val colors = keyColors()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KeyGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyGap),
        ) {
            Key(
                label = "AC",
                containerColor = colors.function,
                contentColor = colors.functionContent,
                modifier = Modifier.weight(1f),
                contentDescription = "All clear",
                onClick = { onStateChange(state.clear()) },
            )
            Key(
                label = "\u00B1",
                containerColor = colors.function,
                contentColor = colors.functionContent,
                modifier = Modifier.weight(1f),
                contentDescription = "Toggle sign",
                onClick = { onStateChange(state.toggleSign()) },
            )
            Key(
                label = "%",
                containerColor = colors.function,
                contentColor = colors.functionContent,
                modifier = Modifier.weight(1f),
                contentDescription = "Percent",
                onClick = { onStateChange(state.percent()) },
            )
            OperationKey(
                operation = Operation.DIVIDE,
                state = state,
                colors = colors,
                modifier = Modifier.weight(1f),
                onStateChange = onStateChange,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyGap),
        ) {
            listOf(7, 8, 9).forEach { digit ->
                DigitKey(digit, state, colors, Modifier.weight(1f), onStateChange)
            }
            OperationKey(
                operation = Operation.MULTIPLY,
                state = state,
                colors = colors,
                modifier = Modifier.weight(1f),
                onStateChange = onStateChange,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyGap),
        ) {
            listOf(4, 5, 6).forEach { digit ->
                DigitKey(digit, state, colors, Modifier.weight(1f), onStateChange)
            }
            OperationKey(
                operation = Operation.SUBTRACT,
                state = state,
                colors = colors,
                modifier = Modifier.weight(1f),
                onStateChange = onStateChange,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyGap),
        ) {
            listOf(1, 2, 3).forEach { digit ->
                DigitKey(digit, state, colors, Modifier.weight(1f), onStateChange)
            }
            OperationKey(
                operation = Operation.ADD,
                state = state,
                colors = colors,
                modifier = Modifier.weight(1f),
                onStateChange = onStateChange,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyGap),
        ) {
            Key(
                label = "0",
                containerColor = colors.digit,
                contentColor = colors.digitContent,
                // The wide "0" key spans two columns, just like on iOS.
                modifier = Modifier.weight(2f),
                contentDescription = "0",
                onClick = { onStateChange(state.input(0)) },
            )
            Key(
                label = ".",
                containerColor = colors.digit,
                contentColor = colors.digitContent,
                modifier = Modifier.weight(1f),
                contentDescription = "Decimal point",
                onClick = { onStateChange(state.decimal()) },
            )
            Key(
                label = "=",
                containerColor = colors.operation,
                contentColor = colors.operationContent,
                modifier = Modifier.weight(1f),
                contentDescription = "Equals",
                onClick = { onStateChange(state.evaluate()) },
            )
        }
    }
}

@Composable
private fun DigitKey(
    digit: Int,
    state: CalculatorState,
    colors: KeyColors,
    modifier: Modifier = Modifier,
    onStateChange: (CalculatorState) -> Unit,
) {
    Key(
        label = digit.toString(),
        containerColor = colors.digit,
        contentColor = colors.digitContent,
        modifier = modifier,
        contentDescription = digit.toString(),
        onClick = { onStateChange(state.input(digit)) },
    )
}

@Composable
private fun OperationKey(
    operation: Operation,
    state: CalculatorState,
    colors: KeyColors,
    modifier: Modifier = Modifier,
    onStateChange: (CalculatorState) -> Unit,
) {
    val selected = state.pendingOperation == operation
    Key(
        label = operation.symbol,
        containerColor = if (selected) colors.selectedOperation else colors.operation,
        contentColor = if (selected) colors.operation else colors.operationContent,
        modifier = modifier,
        contentDescription = operation.accessibilityLabel,
        onClick = { onStateChange(state.setOperation(operation)) },
    )
}

private val Operation.accessibilityLabel: String
    get() = when (this) {
        Operation.ADD -> "Plus"
        Operation.SUBTRACT -> "Minus"
        Operation.MULTIPLY -> "Multiply"
        Operation.DIVIDE -> "Divide"
    }

// MARK: - A single key

/** One round button on the keypad. */
@Composable
private fun Key(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier
            .height(KeyHeight)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

// MARK: - Colors

@Composable
private fun keyColors(): KeyColors {
    val dark = isSystemInDarkTheme()
    return if (dark) {
        KeyColors(
            digit = Color(0xFF2C2C2E),
            digitContent = Color(0xFFF5F5F7),
            function = Color(0xFF3A3A3C),
            functionContent = Color(0xFFF5F5F7),
            operation = CalculatorOrange,
            operationContent = Color.White,
            selectedOperation = Color(0xFFF5F5F7),
        )
    } else {
        KeyColors(
            digit = Color.White,
            digitContent = Color(0xFF111114),
            function = Color(0xFFE3E3E9),
            functionContent = Color(0xFF111114),
            operation = CalculatorOrange,
            operationContent = Color.White,
            selectedOperation = Color.White,
        )
    }
}

private data class KeyColors(
    val digit: Color,
    val digitContent: Color,
    val function: Color,
    val functionContent: Color,
    val operation: Color,
    val operationContent: Color,
    val selectedOperation: Color,
)

// MARK: - Previews

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Light")
@Composable
private fun CalculatorScreenPreview() {
    KotlinCalculatorTheme(darkTheme = false) {
        CalculatorScreen()
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CalculatorScreenDarkPreview() {
    KotlinCalculatorTheme(darkTheme = true) {
        CalculatorScreen()
    }
}
