package com.stemtutor.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stemtutor.app.gallery.GalleryRoute

private const val GALLERY = "gallery"

/** Debug builds open the component gallery, the M0 demo. Role graphs are added from M1. */
@Composable
fun AppEntry() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = GALLERY) {
        composable(GALLERY) { GalleryRoute() }
    }
}
