package com.fakeprofilescan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fakeprofilescan.app.ui.AppRoot
import com.fakeprofilescan.app.ui.theme.FakeProfileScanTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // bar icon contrast follows the system theme, which matches our theme
        setContent { FakeProfileScanTheme { AppRoot(vm) } }
    }
}
