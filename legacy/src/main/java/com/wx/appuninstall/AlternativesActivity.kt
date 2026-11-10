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
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes

/**
 * 演示四：替代方案列表。每张卡片点开都跳到 README 的「Modern alternatives」一节
 * （官方出处与限制在那里集中维护，应用里不重复）。
 *
 * @author venshine
 */
class AlternativesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alternatives)
        setToolbar(R.string.demo_alternatives, "README")
        bind(R.id.alt_fcm, R.string.alt_fcm, R.string.alt_fcm_desc)
        bind(R.id.alt_analytics, R.string.alt_analytics, R.string.alt_analytics_desc)
        bind(R.id.alt_play, R.string.alt_play, R.string.alt_play_desc)
        bind(R.id.alt_heartbeat, R.string.alt_heartbeat, R.string.alt_heartbeat_desc)
        findViewById<View>(R.id.alt_readme).setOnClickListener { openUrl(README_URL) }
    }

    private fun bind(@IdRes id: Int, @StringRes title: Int, @StringRes desc: Int) {
        val card = findViewById<View>(id)
        card.findViewById<TextView>(R.id.alt_title).setText(title)
        card.findViewById<TextView>(R.id.alt_desc).setText(desc)
        card.setOnClickListener { openUrl(README_URL) }
    }

    companion object {
        const val README_URL = "https://github.com/venshine/AppUninstall#modern-alternatives"
    }
}
