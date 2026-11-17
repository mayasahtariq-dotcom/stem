package com.stemtutor.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.stemtutor.app.di.DevServerSettings
import com.stemtutor.app.gallery.GalleryRoute
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChoiceCard
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemColors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

private const val GALLERY = "debug/gallery"

/** Debug builds only: the M0 component gallery stays reachable from Profile. */
fun NavGraphBuilder.debugDestinations(navController: NavHostController) {
    composable(GALLERY) { GalleryRoute() }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugEntryPoint {
    fun dataStore(): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    fun apiConfig(): com.stemtutor.core.network.ApiConfig
}

@Composable
fun DebugProfileExtras(navController: NavHostController) {
    ServerSetting()
    ChunkyButton(R.string.gallery_title, { navController.navigate(GALLERY) }, Modifier.fillMaxWidth(), ChunkyButtonStyle.White)
}

/**
 * Pretend AI (built in) or a real backend on a PC on the same Wi-Fi. Saving restarts the app,
 * because the choice is read once at start. The user signs in again after switching.
 */
@Composable
private fun ServerSetting() {
    val context = LocalContext.current
    val entry = remember { EntryPointAccessors.fromApplication(context.applicationContext, DebugEntryPoint::class.java) }
    val current = remember { entry.apiConfig() }
    var useReal by remember { mutableStateOf(current.useRealApi) }
    var url by remember { mutableStateOf(if (current.useRealApi) current.baseUrl else "") }
    var invalid by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        entry.dataStore().data.first()[DevServerSettings.BASE_URL]?.let { if (url.isEmpty()) url = it }
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StemColors.White).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BilingualText(R.string.dev_server_title, size = BilingualTextSize.Large)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceCard(!useReal, { useReal = false }, Modifier.weight(1f)) { BilingualText(R.string.dev_server_fake, size = BilingualTextSize.Small) }
            ChoiceCard(useReal, { useReal = true }, Modifier.weight(1f)) { BilingualText(R.string.dev_server_real, size = BilingualTextSize.Small) }
        }
        if (useReal) {
            val hint = bilingualStringResource(R.string.dev_server_url_hint)
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    invalid = false
                },
                label = { Text("${hint.regional} · ${hint.english}") },
                placeholder = { Text("http://192.168.1.5:8000/") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            if (invalid) ErrorLine(bilingualStringResource(R.string.dev_server_url_invalid))
        }
        ChunkyButton(
            R.string.dev_server_apply,
            {
                val normalized = if (useReal) DevServerSettings.normalize(url) else null
                if (useReal && normalized == null) {
                    invalid = true
                } else {
                    scope.launch {
                        entry.dataStore().edit { prefs ->
                            prefs[DevServerSettings.USE_REAL] = useReal
                            normalized?.let { prefs[DevServerSettings.BASE_URL] = it }
                        }
                        restart(context)
                    }
                }
            },
            Modifier.fillMaxWidth(),
        )
    }
}

private fun restart(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(launch)
    exitProcess(0)
}
