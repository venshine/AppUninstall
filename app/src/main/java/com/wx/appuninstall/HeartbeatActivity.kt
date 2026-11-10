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
import com.wx.appuninstall.common.BaseActivity
import com.wx.appuninstall.common.Ui
import com.wx.appuninstall.common.Ui.add
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 演示三：自建心跳。前台每 10 秒上报一次（页面不可见时自动停止），后台用 WorkManager 每 15 分钟一次。
 * 服务端按超时判定疑似流失，这里可以查询本机的判定结果。
 *
 * @author venshine
 */
class HeartbeatActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private lateinit var resultView: TextView
    private lateinit var toggle: TextView

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            send()
            handler.postDelayed(this, FOREGROUND_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = buildPage(getString(R.string.hb_title), "WorkManager")

        page.add(Ui.body(this, getString(R.string.hb_desc)), 16)
        val card = Ui.card(this)
        card.add(Ui.caption(this, getString(R.string.hb_id_label)), 0)
        card.add(Ui.mono(this, InstallId.get(this)).apply {
            id = R.id.hb_install_id
            setTextIsSelectable(true)
        }, 8)
        page.add(card, 12)

        toggle = Ui.button(this, getString(R.string.hb_start)).apply {
            id = R.id.hb_foreground
            setOnClickListener { setForeground(!running) }
        }
        page.add(toggle, 16)
        page.add(Ui.button(this, getString(R.string.hb_schedule), outline = true).apply {
            id = R.id.hb_schedule
            setOnClickListener {
                HeartbeatScheduler.schedule(this@HeartbeatActivity)
                resultView.text = getString(R.string.hb_scheduled)
            }
        }, 12)
        page.add(Ui.button(this, getString(R.string.hb_cancel), outline = true).apply {
            id = R.id.hb_cancel
            setOnClickListener {
                HeartbeatScheduler.cancel(this@HeartbeatActivity)
                resultView.text = getString(R.string.hb_cancelled)
            }
        }, 12)
        page.add(Ui.button(this, getString(R.string.hb_check), outline = true).apply {
            id = R.id.hb_check
            setOnClickListener { check() }
        }, 12)

        resultView = Ui.note(this, getString(R.string.hb_idle)).apply { id = R.id.hb_result }
        page.add(resultView, 16)
        page.add(Ui.note(this, getString(R.string.server_hint)), 8)
    }

    override fun onPause() {
        super.onPause()
        setForeground(false)
    }

    private fun setForeground(on: Boolean) {
        running = on
        toggle.text = getString(if (on) R.string.hb_stop else R.string.hb_start)
        handler.removeCallbacks(tick)
        if (on) handler.post(tick)
    }

    private fun send() {
        val id = InstallId.get(this)
        Bg.run({ ServerApi.heartbeat(id) }) { resultView.text = "${now()} ${describe(it)}" }
    }

    private fun check() {
        val id = InstallId.get(this)
        Bg.run({ ServerApi.devices() }) { reply ->
            resultView.text = if (reply.ok) "${now()} " + statusLine(reply.body, id) else describe(reply)
        }
    }

    /** 从 /devices 的回复里找出本机，给出判定状态 */
    private fun statusLine(body: String, id: String): String {
        val json = JSONObject(body)
        val list = json.getJSONArray("devices")
        for (i in 0 until list.length()) {
            val d = list.getJSONObject(i)
            if (d.getString("installId") == id) {
                return "服务端判定：${d.getString("status")}（${d.getLong("lastSeenSecondsAgo")} 秒前见过，超时阈值 ${json.getLong("timeoutSeconds")} 秒）"
            }
        }
        return "服务端还没有本机记录，先上报一次心跳。"
    }

    private fun now() = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

    private companion object {
        const val FOREGROUND_INTERVAL_MS = 10_000L
    }
}
