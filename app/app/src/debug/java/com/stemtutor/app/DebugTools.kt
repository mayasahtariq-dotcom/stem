package com.stemtutor.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.stemtutor.app.gallery.GalleryRoute
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle

private const val GALLERY = "debug/gallery"

/** Debug builds only: the M0 component gallery stays reachable from Profile. */
fun NavGraphBuilder.debugDestinations(navController: NavHostController) {
    composable(GALLERY) { GalleryRoute() }
}

@Composable
fun DebugProfileExtras(navController: NavHostController) {
    ChunkyButton(R.string.gallery_title, { navController.navigate(GALLERY) }, Modifier.fillMaxWidth(), ChunkyButtonStyle.White)
}
