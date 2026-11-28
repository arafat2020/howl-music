package com.howl.music.media

import android.os.Bundle
import androidx.media3.session.SessionCommand

object PlaybackQueue {
    val edit = SessionCommand("com.howl.music.queue.EDIT", Bundle.EMPTY)

    enum class Action { AddNext, AddNow, Play, Remove, MoveEarlier, MoveLater, MoveNext, MoveTo }

    const val ACTION = "action"
    const val ENTRY_ID = "entryId"
    const val TARGET_ID = "targetId"
    const val MEDIA = "media"
}
