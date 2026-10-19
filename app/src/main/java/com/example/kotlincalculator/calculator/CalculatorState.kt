package com.example.kotlincalculator.calculator

import java.io.Serializable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * The four operations the keypad offers.
 *
 * `apply` is the only place arithmetic happens, so the engine below stays
 * focused on what the user is typing.
 */
enum class Operation(val symbol: String) {
    ADD("+"),
    SUBTRACT("\u2212"),
    MULTIPLY("\u00D7"),
    DIVIDE("\u00F7");

    /** Combines two numbers. Dividing by zero gives `NaN`, shown as "Error". */
    fun apply(lhs: Double, rhs: Double): Double = when (this) {
        ADD -> lhs + rhs
        SUBTRACT -> lhs - rhs
        MULTIPLY -> lhs * rhs
        DIVIDE -> if (rhs == 0.0) Double.NaN else lhs / rhs
    }
}

/**
 * The calculator's brain — an immutable state machine with no Android or
 * Compose imports, so it can be unit-tested as plain Kotlin.
 *
 * Every button is a function that takes the current state and returns the next
 * one; the Compose screen simply replaces its state with the result.
 */
data class CalculatorState(
    internal val currentValue: Double = 0.0,
    internal val typedText: String? = null,
    internal val leftOperand: Double? = null,
    val pendingOperation: Operation? = null,
) : Serializable {

    /** What the display shows right now. */
    val display: String
        get() = typedText?.let(::formatTyped) ?: format(currentValue)

    /** The number on screen. "0." and "12." are valid in-progress entries. */
    private val displayValue: Double
        get() = when (val text = typedText) {
            null -> currentValue
            else -> text.toDoubleOrNull()
                ?: text.dropLast(1).toDoubleOrNull()
                ?: currentValue
        }

    /** Appends a digit to the number being typed, or starts a new number. */
    fun input(digit: Int): CalculatorState {
        require(digit in 0..9) { "digit must be between 0 and 9" }
        val text = typedText ?: return copy(typedText = digit.toString())

        // Keep the number readable, just like the 9-digit limit on iOS.
        if (text.count { it.isDigit() } >= MAX_DIGITS) return this

        val next = when (text) {
            "0" -> digit.toString()
            else -> text + digit
        }
        return copy(typedText = next)
    }

    /** Adds a decimal point to the number being typed. */
    fun decimal(): CalculatorState {
        val text = typedText ?: return copy(typedText = "0.")
        if (text.contains('.')) return this
        return copy(typedText = "$text.")
    }

    /** Flips the sign of the number on screen. */
    fun toggleSign(): CalculatorState {
        val text = typedText
            ?: return copy(currentValue = -currentValue)
        if (text == "0") return this // never show "-0"
        val next = if (text.startsWith("-")) text.drop(1) else "-$text"
        return copy(typedText = next)
    }

    /** Turns the number on screen into a percentage. */
    fun percent(): CalculatorState {
        val pending = pendingOperation
        val left = leftOperand
        val value = if (pending != null && left != null &&
            (pending == Operation.ADD || pending == Operation.SUBTRACT)
        ) {
            // 200 + 10 % means "10 percent of 200", i.e. 20.
            left * displayValue / 100
        } else {
            displayValue / 100
        }
        return copy(currentValue = value, typedText = null)
    }

    /** Stores the left-hand number and remembers what to do with the next one. */
    fun setOperation(operation: Operation): CalculatorState {
        val pending = pendingOperation
        val left = leftOperand
        // Chaining (2 + 3 +) settles the previous operation first.
        val value = if (pending != null && left != null && typedText != null) {
            pending.apply(left, displayValue)
        } else {
            displayValue
        }
        return copy(
            currentValue = value,
            typedText = null,
            leftOperand = value,
            pendingOperation = operation,
        )
    }

    /** Works out the pending operation and shows the answer. */
    fun evaluate(): CalculatorState {
        val pending = pendingOperation ?: return this
        val left = leftOperand ?: return this
        return copy(
            currentValue = pending.apply(left, displayValue),
            typedText = null,
            leftOperand = null,
            pendingOperation = null,
        )
    }

    /** Resets everything back to a fresh "0". */
    fun clear(): CalculatorState = CalculatorState()

    private companion object {
        const val MAX_DIGITS = 9

        val decimalFormat = DecimalFormat("#,##0.########", DecimalFormatSymbols(Locale.US))
        val scientificFormat = DecimalFormat("0.########E0", DecimalFormatSymbols(Locale.US))

        /** Renders a number with thousands separators, as 1,234.5. */
        fun format(value: Double): String {
            if (!value.isFinite()) return "Error"
            if (value == 0.0) return "0" // also catches -0.0

            val magnitude = abs(value)
            return when {
                magnitude >= 1e12 || magnitude < 1e-8 -> scientificFormat.format(value)
                else -> decimalFormat.format(value)
            }
        }

        /**
         * Groups the digits the user is still typing while leaving the decimal
         * point exactly where they put it: "1234." becomes "1,234.".
         */
        fun formatTyped(text: String): String {
            val negative = text.startsWith("-")
            val unsigned = if (negative) text.drop(1) else text
            val integerPart = unsigned.substringBefore('.')
            val hasDot = unsigned.contains('.')

            val grouped = integerPart
                .reversed()
                .chunked(3)
                .joinToString(",")
                .reversed()

            return buildString {
                if (negative) append('-')
                append(grouped.ifEmpty { "0" })
                if (hasDot) {
                    append('.')
                    append(unsigned.substringAfter('.'))
                }
            }
        }
    }
}
