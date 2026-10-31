package io.github.harieedawwn.clookie.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.data.UpdateInfo
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.ui.components.ButtonKind
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.update.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object Latest : UpdateUi
    data class Available(val info: UpdateInfo) : UpdateUi
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateUi
    data class Ready(val info: UpdateInfo, val apk: File) : UpdateUi
    data class Failed(val info: UpdateInfo?) : UpdateUi
}

/** Update flow for the Home banner and the Settings card. */
@Stable
class UpdateController(private val context: Context, private val scope: CoroutineScope) {
    var state by mutableStateOf<UpdateUi>(UpdateUi.Idle)
        private set

    fun check(force: Boolean) {
        if (state is UpdateUi.Downloading) return
        if (force) state = UpdateUi.Checking
        scope.launch {
            val found = Updater.check(context, force)
            state = when {
                found != null -> UpdateUi.Available(found)
                force -> UpdateUi.Latest
                else -> UpdateUi.Idle
            }
        }
    }

    /** The banner's single button: download, then install. */
    fun act() {
        when (val s = state) {
            is UpdateUi.Available -> download(s.info)
            is UpdateUi.Failed -> if (s.info != null) download(s.info) else check(force = true)
            is UpdateUi.Ready -> Updater.install(context, s.apk)
            UpdateUi.Idle, UpdateUi.Latest -> check(force = true)
            else -> Unit
        }
    }

    private fun download(info: UpdateInfo) {
        state = UpdateUi.Downloading(info, 0f)
        scope.launch {
            val apk = Updater.download(context, info) { p -> state = UpdateUi.Downloading(info, p) }
            state = if (apk != null) UpdateUi.Ready(info, apk).also { Updater.install(context, apk) } else UpdateUi.Failed(info)
        }
    }
}

@Composable
fun updateActionText(state: UpdateUi): String = when (state) {
    is UpdateUi.Available -> stringResource(R.string.update_download)
    is UpdateUi.Downloading -> stringResource(R.string.update_downloading, (state.progress * 100).toInt())
    is UpdateUi.Ready -> stringResource(R.string.update_install)
    is UpdateUi.Failed -> stringResource(R.string.update_failed)
    UpdateUi.Checking -> stringResource(R.string.update_checking)
    UpdateUi.Latest, UpdateUi.Idle -> stringResource(R.string.action_check)
}

fun UpdateUi.info(): UpdateInfo? = when (this) {
    is UpdateUi.Available -> info
    is UpdateUi.Downloading -> info
    is UpdateUi.Ready -> info
    is UpdateUi.Failed -> info
    else -> null
}

/** Small inline banner on Home while an update is available. */
@Composable
fun UpdateBanner(state: UpdateUi, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val info = state.info() ?: return
    val c = Clookie.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(c.accentSoft, Clookie.shapes.medium)
            .padding(start = Space.s3, end = Space.s2, top = Space.s2, bottom = Space.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClookieIcon(ClookieIcons.Download, null, tint = c.onAccentSoft)
        Column(Modifier.weight(1f).padding(horizontal = Space.s3)) {
            Txt(stringResource(R.string.update_available, info.version), Clookie.type.bodyStrong, color = c.onAccentSoft, maxLines = 1)
        }
        CookieButton(updateActionText(state), onAction, kind = ButtonKind.Secondary, enabled = state !is UpdateUi.Downloading)
    }
}
