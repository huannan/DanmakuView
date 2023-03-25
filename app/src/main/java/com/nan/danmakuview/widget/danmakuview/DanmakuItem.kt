package com.nan.danmakuview.widget.danmakuview

import android.animation.ObjectAnimator
import android.view.View

data class DanmakuItem(
    val line: Int,
    val content: String,
    val itemView: View,
    val animator: ObjectAnimator,
)