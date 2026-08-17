package com.foxcode.foxweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.foxcode.foxweather.ui.RainPrototypeScreen
import com.foxcode.foxweather.ui.theme.FoxWeatherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoxWeatherTheme {
                RainPrototypeScreen()
            }
        }
    }
}