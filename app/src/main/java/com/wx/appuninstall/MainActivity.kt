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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.wx.appuninstall.common.BaseActivity
import com.wx.appuninstall.common.R as CR
import com.wx.appuninstall.common.Ui
import com.wx.appuninstall.common.Ui.add

/**
 * 首页：说明系统不提供卸载回调，列出本应用演示的三种事后感知方案和对比页
 *
 * @author venshine
 */
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        val page = buildPage(getString(R.string.app_name), "v$version", home = true)

        page.add(Ui.text(this, CR.style.Text_H1, getString(R.string.home_title)), 24)
        page.add(Ui.body(this, getString(R.string.home_desc)), 12)

        val configured = FirebaseStatus.isConfigured(this)
        page.add(Ui.chip(this, "Firebase " + getString(if (configured) R.string.configured else R.string.not_configured), configured).apply {
            id = R.id.firebase_chip
        }, 16, wrap = true)
        page.add(Ui.mono(this, getString(R.string.home_server, ServerConfig.BASE_URL)), 8)

        card(page, R.id.card_fcm, CR.drawable.ic_pulse, R.string.demo_fcm, R.string.demo_fcm_desc, FcmActivity::class.java)
        card(page, R.id.card_analytics, CR.drawable.ic_flow, R.string.demo_analytics, R.string.demo_analytics_desc, AnalyticsActivity::class.java)
        card(page, R.id.card_heartbeat, CR.drawable.ic_trash, R.string.demo_heartbeat, R.string.demo_heartbeat_desc, HeartbeatActivity::class.java)
        card(page, R.id.card_compare, CR.drawable.ic_list, R.string.demo_compare, R.string.demo_compare_desc, CompareActivity::class.java)

        page.add(Ui.note(this, getString(R.string.home_observer)), 20)
        page.add(Ui.caption(this, getString(R.string.home_author)), 16)
    }

    private fun card(parent: android.widget.LinearLayout, @IdRes id: Int, @DrawableRes icon: Int,
                     @StringRes title: Int, @StringRes desc: Int, target: Class<out Activity>) {
        val card = Ui.card(this).apply {
            this.id = id
            isClickable = true
            isFocusable = true
            setOnClickListener { startActivity(Intent(this@MainActivity, target)) }
        }
        card.addView(ImageView(this).apply {
            setImageResource(icon)
            imageTintList = android.content.res.ColorStateList.valueOf(getColor(CR.color.accent_ink))
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, android.widget.LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24)))
        card.add(Ui.text(this, CR.style.Text_CardTitle, getString(title)), 14)
        card.add(Ui.text(this, CR.style.Text_CardDesc, getString(desc)), 6)
        parent.add(card, 12)
    }
}
