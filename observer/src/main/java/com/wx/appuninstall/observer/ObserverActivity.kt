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
package com.wx.appuninstall.observer

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wx.appuninstall.common.BaseActivity
import com.wx.appuninstall.common.R as CR
import com.wx.appuninstall.common.Ui
import com.wx.appuninstall.common.Ui.add
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 观察者页面：设置监听的包名，显示它是否已安装，列出收到的移除事件。
 * 打开期间额外动态注册 PACKAGE_REMOVED，用来区分归档与更新。
 *
 * @author venshine
 */
class ObserverActivity : BaseActivity() {

    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var events: LinearLayout
    private val removedReceiver = RemovalReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        val page = buildPage(getString(R.string.title), "v$version", home = true)

        page.add(Ui.body(this, getString(R.string.desc)), 20)
        page.add(Ui.caption(this, getString(R.string.target_label)), 20)
        input = EditText(this, null, 0, CR.style.Text_Mono).apply {
            id = R.id.target_input
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setBackgroundResource(CR.drawable.bg_button_outline)
            val p = Ui.dp(context, 14)
            setPadding(p, p, p, p)
            setText(EventStore.target(context))
        }
        page.add(input, 8)
        page.add(Ui.button(this, getString(R.string.target_save)).apply {
            id = R.id.target_save
            setOnClickListener {
                EventStore.setTarget(context, input.text.toString())
                input.setText(EventStore.target(context))
                refresh()
            }
        }, 12)
        status = Ui.mono(this, "").apply { id = R.id.target_status }
        page.add(status, 12)

        page.add(Ui.section(this, getString(R.string.events_title)), 24)
        events = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            id = R.id.events_container
        }
        page.add(events, 0)
        page.add(Ui.button(this, getString(R.string.clear), outline = true).apply {
            id = R.id.events_clear
            setOnClickListener { EventStore.clear(context) }
        }, 16)
        page.add(Ui.note(this, getString(R.string.notes)), 20)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(removedReceiver, IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply { addDataScheme("package") })
        EventStore.onChange = { refresh() }
        refresh()
    }

    override fun onStop() {
        super.onStop()
        EventStore.onChange = null
        unregisterReceiver(removedReceiver)
    }

    private fun refresh() {
        val target = EventStore.target(this)
        status.text = installedText(this, target)
        events.removeAllViews()
        val list = EventStore.all(this)
        if (list.isEmpty()) {
            events.add(Ui.note(this, getString(R.string.events_empty)), 12)
            return
        }
        val format = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)
        for (e in list) {
            val card = Ui.card(this, 14)
            card.add(Ui.chip(this, e.type.name, e.type == RemovalType.UNINSTALLED), 0, wrap = true)
            card.add(Ui.mono(this, "${e.packageName}  uid=${e.uid}\n${format.format(Date(e.timeMillis))}"), 8)
            events.add(card, 10)
        }
    }

    private companion object {
        /** 目标是否已安装；Android 11+ 要在清单声明 <queries> 才查得到别的包 */
        fun installedText(context: Context, target: String): String = try {
            val info = context.packageManager.getPackageInfo(target, 0)
            "$target  " + context.getString(R.string.installed, info.versionName.orEmpty())
        } catch (e: PackageManager.NameNotFoundException) {
            "$target  " + context.getString(R.string.not_installed)
        }
    }
}
