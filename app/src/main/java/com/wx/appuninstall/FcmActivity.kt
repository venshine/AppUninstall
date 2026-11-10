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
import android.widget.TextView
import com.google.firebase.messaging.FirebaseMessaging
import com.wx.appuninstall.common.BaseActivity
import com.wx.appuninstall.common.Ui
import com.wx.appuninstall.common.Ui.add

/**
 * 演示一：获取 FCM 注册令牌，登记到本地服务端。
 * 依据：https://firebase.google.com/docs/cloud-messaging/android/get-started
 *
 * @author venshine
 */
class FcmActivity : BaseActivity() {

    private lateinit var tokenView: TextView
    private lateinit var resultView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = buildPage(getString(R.string.fcm_title), "FCM")
        val configured = FirebaseStatus.isConfigured(this)

        page.add(Ui.body(this, getString(R.string.fcm_desc)), 16)
        page.add(Ui.chip(this, "Firebase " + getString(if (configured) R.string.configured else R.string.not_configured), configured), 12, wrap = true)

        val card = Ui.card(this)
        card.add(Ui.caption(this, getString(R.string.fcm_token_label)), 0)
        tokenView = Ui.mono(this, AppState.token(this) ?: getString(R.string.fcm_token_empty)).apply {
            id = R.id.fcm_token
            setTextIsSelectable(true)
        }
        card.add(tokenView, 8)
        page.add(card, 12)

        page.add(Ui.button(this, getString(R.string.fcm_fetch)).apply {
            id = R.id.fcm_fetch
            setOnClickListener { fetch(configured) }
        }, 16)
        page.add(Ui.button(this, getString(R.string.fcm_register), outline = true).apply {
            id = R.id.fcm_register
            setOnClickListener { register() }
        }, 12)

        resultView = Ui.note(this, if (configured) getString(R.string.server_hint) else getString(R.string.need_firebase)).apply {
            id = R.id.fcm_result
        }
        page.add(resultView, 16)
        page.add(Ui.note(this, getString(R.string.fcm_note)), 12)
    }

    private fun fetch(configured: Boolean) {
        if (!configured) {
            resultView.text = getString(R.string.need_firebase)
            return
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                AppState.saveToken(this, task.result)
                tokenView.text = task.result
                resultView.text = getString(R.string.server_hint)
            } else {
                resultView.text = "获取失败：" + task.exception?.message
            }
        }
    }

    private fun register() {
        val token = AppState.token(this)
        if (token == null) {
            resultView.text = getString(R.string.fcm_need_token)
            return
        }
        val id = InstallId.get(this)
        Bg.run({ ServerApi.register(id, token) }) { resultView.text = describe(it) }
    }
}

/** 把服务端回复转成给人看的一行 */
internal fun describe(reply: Reply): String =
    if (reply.ok) "服务端已记录：${reply.body}"
    else "失败（${if (reply.code < 0) "没连上" else "HTTP ${reply.code}"}）${reply.body}\n确认服务端已启动并执行 adb reverse tcp:8080 tcp:8080。"
