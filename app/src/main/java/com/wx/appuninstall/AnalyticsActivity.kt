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
import com.google.firebase.analytics.FirebaseAnalytics
import com.wx.appuninstall.common.BaseActivity
import com.wx.appuninstall.common.Ui
import com.wx.appuninstall.common.Ui.add

/**
 * 演示二：启用 Firebase Analytics，并说明 app_remove 事件在哪里看。
 * 依据：https://support.google.com/analytics/answer/9234069 、https://firebase.google.com/docs/projects/bigquery-export
 *
 * @author venshine
 */
class AnalyticsActivity : BaseActivity() {

    private lateinit var resultView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = buildPage(getString(R.string.analytics_title), "app_remove")
        val configured = FirebaseStatus.isConfigured(this)
        val analytics = if (configured) FirebaseAnalytics.getInstance(this) else null

        page.add(Ui.body(this, getString(R.string.analytics_desc)), 16)
        page.add(Ui.chip(this, "Firebase " + getString(if (configured) R.string.configured else R.string.not_configured), configured), 12, wrap = true)

        val card = Ui.card(this)
        card.add(Ui.caption(this, getString(R.string.analytics_id_label)), 0)
        val idView = Ui.mono(this, "—").apply {
            id = R.id.analytics_id
            setTextIsSelectable(true)
        }
        card.add(idView, 8)
        page.add(card, 12)
        analytics?.appInstanceId?.addOnCompleteListener { if (it.isSuccessful) idView.text = it.result }

        page.add(Ui.button(this, getString(R.string.analytics_enable)).apply {
            id = R.id.analytics_enable
            setOnClickListener {
                analytics?.setAnalyticsCollectionEnabled(true)
                resultView.text = if (analytics != null) getString(R.string.analytics_enabled) else getString(R.string.need_firebase)
            }
        }, 16)
        page.add(Ui.button(this, getString(R.string.analytics_event), outline = true).apply {
            id = R.id.analytics_event
            setOnClickListener {
                analytics?.logEvent("demo_open", null)
                resultView.text = if (analytics != null) getString(R.string.analytics_logged) else getString(R.string.need_firebase)
            }
        }, 12)

        resultView = Ui.note(this, if (configured) "" else getString(R.string.need_firebase)).apply { id = R.id.analytics_result }
        page.add(resultView, 16)
        page.add(Ui.note(this, getString(R.string.analytics_steps)), 12)
    }
}
