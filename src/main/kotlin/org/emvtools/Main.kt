package org.emvtools

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.emvtools.ui.MainScreen

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1400.dp, 900.dp)
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Banking and Payment Tools by Apex Gang",
        state = windowState
    ) {
        MainScreen()
    }
}

