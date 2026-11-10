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
import android.widget.TextView

/**
 * 演示二：启动监听。显示 native `watch` 的返回值、监听进程的 pid，以及它此刻是否存活（每秒刷新）。
 *
 * @author venshine
 */
class WatchActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var returned: String? = null

    private val refresh = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, REFRESH_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch)
        setToolbar(R.string.demo_watch, "Uninstall.watch")
        findViewById<TextView>(R.id.watch_start).setOnClickListener { start() }
        findViewById<TextView>(R.id.watch_refresh).setOnClickListener { render() }
        render()
    }

    override fun onStart() {
        super.onStart()
        handler.postDelayed(refresh, REFRESH_MS)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(refresh)
    }

    private fun start() {
        returned = try {
            WatchController.start(this).toString()
        } catch (e: UnsatisfiedLinkError) {
            getString(R.string.watch_error, e.javaClass.simpleName)
        }
        render()
    }

    private fun render() {
        findViewById<TextView>(R.id.watch_return).text = returned ?: getString(R.string.watch_not_started)
        val pid = WatchController.watcherPid(this)
        findViewById<TextView>(R.id.watch_pid).text = pid?.toString() ?: getString(R.string.watch_no_pid)
        val alive = pid != null && WatchController.isAlive(pid)
        findViewById<TextView>(R.id.watch_alive).apply {
            setText(if (alive) R.string.watch_alive else R.string.watch_dead)
            setTextColor(getColor(if (alive) R.color.ok_ink else R.color.bad_ink))
            setBackgroundResource(if (alive) R.drawable.bg_chip_ok else R.drawable.bg_chip_bad)
        }
    }

    private companion object {
        const val REFRESH_MS = 1000L
    }
}
