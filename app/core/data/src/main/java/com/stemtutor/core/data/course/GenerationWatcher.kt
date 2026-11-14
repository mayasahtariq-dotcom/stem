package com.stemtutor.core.data.course

import com.stemtutor.core.data.model.UploadProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** A finished course the user hasn't opened yet, for the in-app "lesson 1 is ready" banner. */
data class ReadyNotice(val uploadId: String, val courseId: String, val firstLessonId: String?, val ok: Boolean)

/**
 * Polls generation progress every [POLL_MS] (PRD: every two seconds), app-wide, so the teacher
 * can leave the generating screen and still be told when lesson 1 is ready. Push notifications
 * replace the banner in M6.
 */
class GenerationWatcher(
    private val courses: CourseRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val pollMs: Long = POLL_MS,
) {
    private val _progress = MutableStateFlow<Map<String, UploadProgress>>(emptyMap())
    val progress: StateFlow<Map<String, UploadProgress>> = _progress.asStateFlow()

    private val _notice = MutableStateFlow<ReadyNotice?>(null)
    val notice: StateFlow<ReadyNotice?> = _notice.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    @Synchronized
    fun watch(initial: UploadProgress) {
        _progress.update { it + (initial.id to initial) }
        if (initial.finished || jobs[initial.id]?.isActive == true) return
        jobs[initial.id] = scope.launch {
            while (isActive) {
                delay(pollMs)
                val latest = courses.progress(initial.id).getOrNull() ?: continue
                _progress.update { it + (latest.id to latest) }
                if (latest.finished) {
                    latest.courseId?.let { _notice.value = ReadyNotice(latest.id, it, latest.firstLessonId, latest.ready) }
                    break
                }
            }
        }
    }

    /** The user saw the result (on the generating screen or by tapping the banner). */
    fun dismiss(uploadId: String) {
        if (_notice.value?.uploadId == uploadId) _notice.value = null
    }

    companion object {
        const val POLL_MS = 2_000L
    }
}
