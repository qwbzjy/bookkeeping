package com.wenbo.bookkeeping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.wenbo.bookkeeping.di.LocalAppContainer
import com.wenbo.bookkeeping.navigation.BookkeepingAppScaffold
import com.wenbo.bookkeeping.ui.theme.BookkeepingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BookkeepingApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                BookkeepingTheme {
                    BookkeepingAppScaffold()
                }
            }
        }
    }
}
