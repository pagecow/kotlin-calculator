package com.example.kotlincalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.kotlincalculator.ui.CalculatorScreen
import com.example.kotlincalculator.ui.theme.KotlinCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotlinCalculatorTheme {
                CalculatorScreen()
            }
        }
    }
}
