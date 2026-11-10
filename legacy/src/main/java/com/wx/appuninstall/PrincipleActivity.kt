/*
 * Copyright (C) 2016 venshine.cn@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wx.appuninstall

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes

/**
 * 演示一：原理分步图示 fork -> inotify -> 卸载 -> 反馈。每一步左边是 2016 年的做法，
 * 右边是新系统上的预期（依据见 README，一台 Android 17 真机实测一致）。可手动下一步，也可自动播放。
 *
 * @author venshine
 */
class PrincipleActivity : BaseActivity() {

    private class Step(@StringRes val then: Int, @StringRes val now: Int)

    private val steps = listOf(
        Step(R.string.step1_then, R.string.step1_now),
        Step(R.string.step2_then, R.string.step2_now),
        Step(R.string.step3_then, R.string.step3_now),
        Step(R.string.step4_then, R.string.step4_now),
    )
    private val stepViews = intArrayOf(R.id.step_1, R.id.step_2, R.id.step_3, R.id.step_4)
    private val handler = Handler(Looper.getMainLooper())
    private var current = -1
    private var playing = false

    private val tick = object : Runnable {
        override fun run() {
            if (current >= steps.lastIndex) {
                setPlaying(false)
            } else {
                select(current + 1)
                handler.postDelayed(this, STEP_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principle)
        setToolbar(R.string.demo_principle, "fork · inotify")
        stepViews.forEachIndexed { index, id ->
            findViewById<android.view.View>(id).setOnClickListener {
                setPlaying(false)
                select(index)
            }
        }
        findViewById<TextView>(R.id.principle_next).setOnClickListener {
            setPlaying(false)
            if (current < steps.lastIndex) select(current + 1)
        }
        findViewById<TextView>(R.id.principle_reset).setOnClickListener {
            setPlaying(false)
            select(-1)
        }
        findViewById<TextView>(R.id.principle_play).setOnClickListener {
            if (playing) setPlaying(false) else {
                if (current >= steps.lastIndex) select(-1)
                setPlaying(true)
            }
        }
        select(-1)
    }

    override fun onStop() {
        super.onStop()
        setPlaying(false)
    }

    private fun setPlaying(on: Boolean) {
        playing = on
        handler.removeCallbacks(tick)
        if (on) handler.post(tick)
        findViewById<TextView>(R.id.principle_play).setText(if (on) R.string.principle_pause else R.string.principle_play)
    }

    /** 选中第 [index] 步（-1 表示都不选），高亮该卡片并更新说明 */
    private fun select(index: Int) {
        current = index
        stepViews.forEachIndexed { i, id ->
            findViewById<android.view.View>(id).isSelected = i == index
        }
        val then = findViewById<TextView>(R.id.principle_then)
        val now = findViewById<TextView>(R.id.principle_now)
        if (index < 0) {
            then.setText(R.string.principle_idle)
            now.text = ""
        } else {
            then.setText(steps[index].then)
            now.setText(steps[index].now)
        }
        findViewById<TextView>(R.id.principle_progress).text = "${index + 1} / ${steps.size}"
        findViewById<android.view.View>(R.id.principle_now_label).visibility =
            if (index < 0) android.view.View.GONE else android.view.View.VISIBLE
    }

    private companion object {
        const val STEP_MS = 1600L
    }
}
