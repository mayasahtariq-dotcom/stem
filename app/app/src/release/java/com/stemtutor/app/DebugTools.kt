package com.stemtutor.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/** Release builds have no debug screens. */
@Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
fun NavGraphBuilder.debugDestinations(navController: NavHostController) = Unit

@Suppress("UNUSED_PARAMETER")
@Composable
fun DebugProfileExtras(navController: NavHostController) = Unit
