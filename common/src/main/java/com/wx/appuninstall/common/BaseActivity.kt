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
package com.wx.appuninstall.common

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 示例页公共外观：Android 15+ 强制全面屏的边距、深浅色切换、顶栏、滚动页面容器
 *
 * @author venshine
 */
abstract class BaseActivity : AppCompatActivity() {

    /**
     * 搭一个带顶栏的滚动页面，返回放内容的纵向容器。
     * [home] 为 true 时显示 logo 和应用名（首页），否则显示返回键和页面标题。
     */
    protected fun buildPage(title: CharSequence, chip: CharSequence? = null, home: Boolean = false): LinearLayout {
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val side = resources.getDimensionPixelSize(R.dimen.page_padding)
            setPadding(side, Ui.dp(context, 16), side, Ui.dp(context, 32))
        }
        scroll.addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        column.addView(toolbar(title, chip, home))
        setContentView(scroll)
        applyWindow(scroll)
        return column
    }

    private fun toolbar(title: CharSequence, chip: CharSequence?, home: Boolean): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 48))
        }
        if (home) {
            bar.addView(ImageView(this).apply {
                setBackgroundResource(R.drawable.bg_logo)
                setImageResource(R.drawable.ic_logo)
                imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.on_accent))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                val p = Ui.dp(context, 7)
                setPadding(p, p, p, p)
            }, LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)))
        } else {
            bar.addView(ImageButton(this, null, 0, R.style.IconButton).apply {
                id = R.id.toolbar_back
                setImageResource(R.drawable.ic_back)
                contentDescription = getString(R.string.back)
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)).also { it.marginStart = -Ui.dp(this, 10) })
        }
        bar.addView(Ui.text(this, if (home) R.style.Text_Numeric else R.style.Text_Title, title).apply {
            id = R.id.toolbar_title
            if (home) textSize = 19f
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
            it.marginStart = Ui.dp(this, if (home) 10 else 4)
        })
        if (chip != null) {
            bar.addView(Ui.text(this, R.style.Text_Mono_Chip, chip).apply { id = R.id.toolbar_chip },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val night = DemoTheme.isNight(this)
        bar.addView(ImageButton(this, null, 0, R.style.IconButton).apply {
            id = R.id.theme_toggle
            setImageResource(if (night) R.drawable.ic_sun else R.drawable.ic_moon)
            contentDescription = getString(if (night) R.string.theme_to_light else R.string.theme_to_dark)
            setOnClickListener { DemoTheme.toggle(this@BaseActivity) }
        }, LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)).also { it.marginStart = Ui.dp(this, 4) })
        return bar
    }

    private fun applyWindow(root: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // targetSdk 35 起，Android 15+ 强制全面屏，内容需要避开状态栏、导航栏和挖孔
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val light = !DemoTheme.isNight(this)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    /** 带系统兜底的外链打开：没有浏览器时弹出网址 */
    protected fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show()
        }
    }
}
