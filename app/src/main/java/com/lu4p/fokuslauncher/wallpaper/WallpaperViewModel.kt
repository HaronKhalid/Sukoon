package com.lu4p.fokuslauncher.wallpaper

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WallpaperUiState(
    val preview: Bitmap? = null,
    val recents: List<File> = emptyList(),
    val averageColor: Int? = null,
    val applyLock: Boolean = false,
    val busy: Boolean = false,
    val message: WallpaperMessage? = null,
)

enum class WallpaperMessage { APPLIED_HOME, APPLIED_HOME_LOCK, LOCK_FAILED, FAILED, LOAD_FAILED }

@HiltViewModel
class WallpaperViewModel
@Inject
constructor(
    private val repo: WallpaperRepository,
    private val prefs: PreferencesManager,
) : ViewModel() {
    private val _state = MutableStateFlow(WallpaperUiState())
    val state: StateFlow<WallpaperUiState> = _state

    init {
        refreshRecents()
    }

    private fun refreshRecents() {
        viewModelScope.launch { _state.update { it.copy(recents = repo.recents()) } }
    }

    private fun show(bmp: Bitmap) {
        _state.update { it.copy(preview = bmp, averageColor = repo.averageOf(bmp)) }
    }

    private fun loadFailed() {
        _state.update { it.copy(message = WallpaperMessage.LOAD_FAILED) }
    }

    fun picked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val bmp = repo.saveReference(uri)?.let { repo.decodeFile(it, 2048) }
            if (bmp == null) loadFailed() else show(bmp)
            refreshRecents()
        }
    }

    fun selectRecent(file: File) {
        viewModelScope.launch {
            val bmp = repo.decodeFile(file, 2048)
            if (bmp == null) loadFailed() else show(bmp)
        }
    }

    fun selectBuiltIn(w: BuiltInWallpaper) = show(repo.builtIn(w))

    fun deleteRecent(file: File) {
        viewModelScope.launch {
            repo.delete(file)
            refreshRecents()
        }
    }

    fun setApplyLock(v: Boolean) = _state.update { it.copy(applyLock = v) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun apply() {
        val bmp = _state.value.preview ?: return
        val lock = _state.value.applyLock
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val r = repo.apply(bmp, home = true, lock = lock)
            if (r.homeApplied) prefs.setHomeUsesPhotoWallpaper(true)
            val msg =
                when {
                    !r.homeApplied -> WallpaperMessage.FAILED
                    lock && !r.lockApplied -> WallpaperMessage.LOCK_FAILED
                    lock -> WallpaperMessage.APPLIED_HOME_LOCK
                    else -> WallpaperMessage.APPLIED_HOME
                }
            _state.update { it.copy(busy = false, message = msg) }
        }
    }
}
