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
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes

/**
 * 首页：说明这是 2016 年的技术演示，以及它在当前系统上的预期结果；四个演示页入口
 *
 * @author venshine
 */
class MainActivity : BaseActivity() {

    /** 版本号只在 app/build.gradle 的 versionName 维护一处 */
    private val version: String by lazy {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.source_button).setOnClickListener { openUrl(SOURCE_URL) }
        findViewById<TextView>(R.id.version_chip).text = "v$version"
        findViewById<TextView>(R.id.preview_device).text =
            getString(R.string.home_device, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
        findViewById<View>(R.id.card_principle).setOnClickListener { open(PrincipleActivity::class.java) }
        bindCard(R.id.card_watch, R.drawable.ic_pulse, R.string.demo_watch, R.string.demo_watch_desc,
            WatchActivity::class.java)
        bindCard(R.id.card_uninstall, R.drawable.ic_trash, R.string.demo_uninstall, R.string.demo_uninstall_desc,
            UninstallActivity::class.java)
        bindCard(R.id.card_alternatives, R.drawable.ic_list, R.string.demo_alternatives,
            R.string.demo_alternatives_desc, AlternativesActivity::class.java)
    }

    private fun bindCard(@IdRes id: Int, @DrawableRes icon: Int, @StringRes title: Int, @StringRes desc: Int,
                         target: Class<out Activity>) {
        val card = findViewById<View>(id)
        card.findViewById<ImageView>(R.id.card_icon).setImageResource(icon)
        card.findViewById<TextView>(R.id.card_title).setText(title)
        card.findViewById<TextView>(R.id.card_desc).setText(desc)
        card.setOnClickListener { open(target) }
    }

    private fun open(target: Class<out Activity>) = startActivity(Intent(this, target))

    private companion object {
        const val SOURCE_URL = "https://github.com/venshine/AppUninstall"
    }
}
