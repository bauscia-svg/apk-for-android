package com.alextm.filexplorer

import MainFileExplorerScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alextm.filexplorer.ui.theme.FileExplorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FileExplorerTheme {
                MainFileExplorerScreen()
            }
        }
    }
}