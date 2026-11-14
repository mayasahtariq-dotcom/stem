package com.stemtutor.core.data.session

import com.stemtutor.core.data.classroom.JoinKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A join key from a tapped link (stemtutor://join/KEY or https://host/join/KEY), held until the
 * user is signed in as a student and the join screen can take it.
 */
class PendingJoinKey {
    private val _key = MutableStateFlow<String?>(null)
    val key: StateFlow<String?> = _key.asStateFlow()

    /** Accepts any link or text containing a key; ignores anything else. */
    fun offer(text: String?) {
        JoinKeys.findIn(text.orEmpty())?.let { _key.value = it }
    }

    fun consume(): String? = _key.value.also { _key.value = null }
}
