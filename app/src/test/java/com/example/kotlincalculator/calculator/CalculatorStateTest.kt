package com.example.kotlincalculator.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The engine is plain Kotlin, so these tests run on the JVM in milliseconds —
 * no emulator needed. The `press` helper below turns button names into the same
 * calls the Compose screen makes.
 */
class CalculatorStateTest {

    @Test
    fun `starts at zero with nothing pending`() {
        val state = CalculatorState()
        assertEquals("0", state.display)
        assertNull(state.pendingOperation)
    }

    @Test
    fun `typing digits builds the number`() {
        assertEquals("7", CalculatorState().press(7).display)
        assertEquals("12", CalculatorState().press(1, 2).display)
    }

    @Test
    fun `a leading zero is replaced by the first real digit`() {
        assertEquals("7", CalculatorState().press(0, 7).display)
    }

    @Test
    fun `digits are grouped with thousands separators`() {
        assertEquals("1,234,567", CalculatorState().press(1, 2, 3, 4, 5, 6, 7).display)
    }

    @Test
    fun `typing is capped at nine digits`() {
        val state = CalculatorState().press(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        assertEquals("123,456,789", state.display)
    }

    @Test
    fun `decimal point starts and extends a number`() {
        assertEquals("0.", CalculatorState().press('.').display)
        assertEquals("0.5", CalculatorState().press('.', 5).display)
        assertEquals("1,234.", CalculatorState().press(1, 2, 3, 4, '.').display)
    }

    @Test
    fun `a second decimal point is ignored`() {
        assertEquals("1.2", CalculatorState().press(1, '.', 2, '.').display)
    }

    @Test
    fun `toggle sign flips the number without producing negative zero`() {
        assertEquals("-5", CalculatorState().press(5, '±').display)
        assertEquals("5", CalculatorState().press(5, '±', '±').display)
        assertEquals("0", CalculatorState().press('±').display)
        assertEquals("0", CalculatorState().press(0, '±').display)
    }

    @Test
    fun `toggle sign works on a finished result`() {
        assertEquals("-8", CalculatorState().press(5, Operation.ADD, 3, '=', '±').display)
    }

    @Test
    fun `addition`() {
        assertEquals("5", CalculatorState().press(2, Operation.ADD, 3, '=').display)
    }

    @Test
    fun `subtraction can go negative`() {
        assertEquals("-3", CalculatorState().press(2, Operation.SUBTRACT, 5, '=').display)
    }

    @Test
    fun `multiplication`() {
        assertEquals("42", CalculatorState().press(6, Operation.MULTIPLY, 7, '=').display)
    }

    @Test
    fun `division`() {
        assertEquals("4", CalculatorState().press(12, Operation.DIVIDE, 3, '=').display)
    }

    @Test
    fun `division rounds to eight decimal places`() {
        assertEquals("0.33333333", CalculatorState().press(1, Operation.DIVIDE, 3, '=').display)
    }

    @Test
    fun `results are grouped with thousands separators`() {
        val state = CalculatorState().press(1, 2, 3, 4, 5, 6, 7, Operation.ADD, 1, '=')
        assertEquals("1,234,568", state.display)
    }

    @Test
    fun `chained operations settle the previous one first`() {
        val state = CalculatorState()
            .press(2, Operation.ADD, 3, Operation.ADD, 4, '=')
        assertEquals("9", state.display)
    }

    @Test
    fun `pressing a second operation replaces the first`() {
        val state = CalculatorState()
            .press(5, Operation.ADD, Operation.MULTIPLY, 2, '=')
        assertEquals("10", state.display)
    }

    @Test
    fun `division by zero shows Error`() {
        assertEquals("Error", CalculatorState().press(1, Operation.DIVIDE, 0, '=').display)
    }

    @Test
    fun `equals without a pending operation does nothing`() {
        assertEquals("7", CalculatorState().press(7, '=').display)
    }

    @Test
    fun `percent turns the current number into a fraction`() {
        assertEquals("0.05", CalculatorState().press(5, '%').display)
    }

    @Test
    fun `percent of the left operand applies to addition`() {
        val state = CalculatorState().press(200, Operation.ADD, 10, '%', '=')
        assertEquals("220", state.display)
    }

    @Test
    fun `percent of the left operand applies to subtraction`() {
        val state = CalculatorState().press(200, Operation.SUBTRACT, 10, '%', '=')
        assertEquals("180", state.display)
    }

    @Test
    fun `percent with multiplication is a plain fraction`() {
        val state = CalculatorState().press(200, Operation.MULTIPLY, 10, '%', '=')
        assertEquals("20", state.display)
    }

    @Test
    fun `clear resets the display and the pending operation`() {
        val state = CalculatorState().press(9, Operation.ADD, 3).clear()
        assertEquals("0", state.display)
        assertNull(state.pendingOperation)
    }

    @Test
    fun `an operation is highlighted until it is resolved`() {
        val state = CalculatorState().press(5, Operation.MULTIPLY)
        assertEquals(Operation.MULTIPLY, state.pendingOperation)
        assertNull(state.press(2, '=').pendingOperation)
    }

    @Test
    fun `the display never shows negative zero`() {
        val state = CalculatorState().press(5, Operation.SUBTRACT, 5, '=', '±')
        assertEquals("0", state.display)
        assertTrue(state.display.toDouble() == 0.0)
    }

    private fun CalculatorState.press(vararg keys: Any): CalculatorState =
        keys.fold(this) { current, key ->
            when (key) {
                is Int -> key.toString().fold(current) { acc, digit -> acc.input(digit.digitToInt()) }
                is Operation -> current.setOperation(key)
                '.' -> current.decimal()
                '=' -> current.evaluate()
                '%' -> current.percent()
                '±' -> current.toggleSign()
                else -> error("Unknown key: $key")
            }
        }
}
