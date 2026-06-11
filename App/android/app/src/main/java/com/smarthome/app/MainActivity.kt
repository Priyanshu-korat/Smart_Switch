package com.smarthome.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.smarthome.app.navigation.AppNavGraph
import com.smarthome.core.designsystem.SmartHomeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timber.log.Timber.d("MainActivity: onCreate started")
        enableEdgeToEdge()
        val navigateTo = intent.getStringExtra("navigate_to")
        timber.log.Timber.d("MainActivity: navigateTo = $navigateTo")

        setContent {
            timber.log.Timber.d("MainActivity: setContent block executing")
            SmartHomeTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    targetDestination = navigateTo
                )
            }
        }
        timber.log.Timber.d("MainActivity: onCreate finished")
    }
}
