package com.kk.reader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kk.reader.ui.library.LibraryScreen
import com.kk.reader.ui.reader.ReaderScreen
import com.kk.reader.ui.settings.SettingsScreen
import com.kk.reader.ui.ttsmodels.TtsModelManagerScreen

object Routes {
    const val LIBRARY = "library"
    const val READER = "reader/{bookId}"
    const val TTS_MODELS = "tts_models"
    const val SETTINGS = "settings"

    fun reader(bookId: Long) = "reader/$bookId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBookClick = { bookId -> navController.navigate(Routes.reader(bookId)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onTtsModelsClick = { navController.navigate(Routes.TTS_MODELS) }
            )
        }

        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) {
            ReaderScreen(
                onBack = { navController.popBackStack() },
                onTtsModelsClick = { navController.navigate(Routes.TTS_MODELS) }
            )
        }

        composable(Routes.TTS_MODELS) {
            TtsModelManagerScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
