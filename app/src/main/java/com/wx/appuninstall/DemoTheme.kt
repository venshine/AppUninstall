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

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * 示例 App 的深浅色切换：选择保存在 SharedPreferences，启动时恢复
 *
 * @author venshine
 */
object DemoTheme {

    private const val PREFS = "demo_theme"
    private const val KEY_DARK = "dark"

    /** 启动时恢复上次的选择，未选择过时为浅色 */
    fun restore(context: Context) {
        AppCompatDelegate.setDefaultNightMode(modeOf(savedDark(context)))
    }

    fun toggle(context: Context) {
        val dark = !isNight(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, dark).apply()
        AppCompatDelegate.setDefaultNightMode(modeOf(dark))
    }

    fun isNight(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun savedDark(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false)

    private fun modeOf(dark: Boolean) =
        if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
}

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DemoTheme.restore(this)
    }
}
